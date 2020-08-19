package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.google.common.util.concurrent.Uninterruptibles;
import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.StorageErrorCodeStrings;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.DeleteSnapshotsOption;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.RejectedBlobCopyException;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);
    private static final String REJECTED_CONTAINER_NAME_SUFFIX = "-rejected";
    private static final String SELECT_ALL_CONTAINER = "ALL";
    private static final String LEASE_ALREADY_ACQUIRED_MESSAGE =
        "Can't acquire lease on file {} in container {} - already acquired";
    public static final String LEASE_EXPIRATION_TIME = "leaseExpirationTime";

    private final CloudBlobClient cloudBlobClient;
    private final BlobServiceClient blobServiceClient;

    private final BlobManagementProperties properties;

    public BlobManager(
        BlobServiceClient blobServiceClient,
        CloudBlobClient cloudBlobClient,
        BlobManagementProperties properties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.cloudBlobClient = cloudBlobClient;
        this.properties = properties;
    }

    public Optional<String> acquireLease(CloudBlockBlob cloudBlockBlob, String containerName, String zipFilename) {
        log.info("Trying to acquire lease on file {} in container {}", zipFilename, containerName);

        try {
            // Note: trying to lease an already leased blob throws an exception and
            // we really do not want to fill the application logs with these. Unfortunately
            // even with this check there is still a chance of an exception as check + lease
            // cannot be expressed as an atomic operation (not that I can see anyway).
            // All considered this should still be much better than not checking lease status
            // at all.
            if (cloudBlockBlob.getProperties().getLeaseStatus() == LeaseStatus.LOCKED) {
                log.info(LEASE_ALREADY_ACQUIRED_MESSAGE, zipFilename, containerName);
                return Optional.empty();
            } else if (!readyToAcquireLease(cloudBlockBlob, zipFilename, containerName)) {
                log.info(
                    "Lease already acquired on file {} in container {} less than {} seconds ago. "
                        + "Lease Expires at: {}",
                    zipFilename,
                    containerName,
                    properties.getBlobLeaseAcquireDelayInSeconds(),
                    cloudBlockBlob.getMetadata().get(LEASE_EXPIRATION_TIME)
                );
                return Optional.empty();
            }

            String leaseId = cloudBlockBlob.acquireLease(properties.getBlobLeaseTimeout(), null);
            log.info("Acquired lease on file {} in container {}. Lease ID: {}", zipFilename, containerName, leaseId);

            // add lease expiration time to the blob metadata
            cloudBlockBlob.getMetadata().put(
                LEASE_EXPIRATION_TIME,
                LocalDateTime.now(EUROPE_LONDON_ZONE_ID)
                    .plusSeconds(properties.getBlobLeaseAcquireDelayInSeconds()).toString()
            );
            cloudBlockBlob.uploadMetadata(AccessCondition.generateLeaseCondition(leaseId), null, null);

            log.info(
                "Updated blob metadata with lease expiration time for file {} in container {}. "
                    + "Lease Expiration Time: {}",
                zipFilename,
                containerName,
                cloudBlockBlob.getMetadata().get(LEASE_EXPIRATION_TIME)
            );

            return Optional.of(leaseId);
        } catch (StorageException storageException) {
            if (storageException.getHttpStatusCode() == HttpStatus.CONFLICT.value()) {
                log.info(LEASE_ALREADY_ACQUIRED_MESSAGE, zipFilename, containerName, storageException);
            } else {
                logAcquireLeaseError(zipFilename, containerName, storageException);
            }
            return Optional.empty();
        } catch (Exception exception) {
            logAcquireLeaseError(zipFilename, containerName, exception);

            return Optional.empty();
        }
    }

    public void tryReleaseLease(
        CloudBlockBlob cloudBlockBlob,
        String containerName,
        String zipFileName,
        String leaseId
    ) {
        try {
            log.info("Releasing lease on file {} in container {}. Lease ID: {}", zipFileName, containerName, leaseId);
            // clear lease expiration time from blob metadata
            cloudBlockBlob.getMetadata().remove(LEASE_EXPIRATION_TIME);
            cloudBlockBlob.uploadMetadata(AccessCondition.generateLeaseCondition(leaseId), null, null);

            cloudBlockBlob.releaseLease(AccessCondition.generateLeaseCondition(leaseId));
            log.info("Released lease on file {} in container {}. Lease ID: {}", zipFileName, containerName, leaseId);
        } catch (Exception exc) {
            log.error(
                "Failed to release the lease on file {} in container {}. Lease ID: {}",
                zipFileName,
                containerName,
                leaseId,
                exc
            );
        }
    }

    public CloudBlobContainer getContainer(String containerName) throws URISyntaxException, StorageException {
        return cloudBlobClient.getContainerReference(containerName);
    }

    public List<CloudBlobContainer> listInputContainers() {
        List<CloudBlobContainer> cloudBlobContainerList = StreamSupport
            .stream(cloudBlobClient.listContainers().spliterator(), false)
            .filter(c -> !c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
            .filter(this::filterBySelectedContainer)
            .collect(toList());

        if (cloudBlobContainerList.isEmpty()) {
            log.error("Container not found for configured container name : {}", properties.getBlobSelectedContainer());
        }

        return cloudBlobContainerList;
    }

    public List<BlobContainerClient> getInputContainerClients() {
        List<BlobContainerClient> blobContainerClientList =
            blobServiceClient
                .listBlobContainers()
                .stream()
                .filter(c -> !c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
                .filter(this::filterBySelectedContainer)
                .map(c -> blobServiceClient.getBlobContainerClient(c.getName()))
                .collect(toList());
        if (blobContainerClientList.isEmpty()) {
            log.error("Container not found for configured container name : {}", properties.getBlobSelectedContainer());
        }
        return blobContainerClientList;
    }

    private boolean filterBySelectedContainer(CloudBlobContainer container) {
        String selectedContainer = properties.getBlobSelectedContainer();
        return SELECT_ALL_CONTAINER.equalsIgnoreCase(selectedContainer)
            || selectedContainer.equals(container.getName());
    }

    private boolean filterBySelectedContainer(BlobContainerItem container) {
        String selectedContainer = properties.getBlobSelectedContainer();
        return SELECT_ALL_CONTAINER.equalsIgnoreCase(selectedContainer)
            || selectedContainer.equals(container.getName());
    }

    public List<BlobContainerClient> getRejectedContainers() {
        return blobServiceClient.listBlobContainers()
            .stream()
            .filter(c -> c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
            .map(c -> blobServiceClient.getBlobContainerClient(c.getName()))
            .collect(toList());
    }

    public void tryMoveFileToRejectedContainer(String fileName, String inputContainerName, String leaseId) {
        String rejectedContainerName = getRejectedContainerName(inputContainerName);

        try {
            moveFileToRejectedContainer(
                fileName,
                inputContainerName,
                rejectedContainerName,
                leaseId
            );
        } catch (Exception ex) {
            log.error(
                "An error occurred when moving rejected file {} from container {} to rejected files' container {}",
                fileName,
                inputContainerName,
                rejectedContainerName,
                ex
            );
        }
    }

    private void moveFileToRejectedContainer(
        String fileName,
        String inputContainerName,
        String rejectedContainerName,
        String leaseId
    ) throws URISyntaxException, StorageException {
        log.info("Moving file {} from container {} to {}", fileName, inputContainerName, rejectedContainerName);
        CloudBlockBlob inputBlob = getBlob(fileName, inputContainerName);
        CloudBlockBlob rejectedBlob = getBlob(fileName, rejectedContainerName);
        if (rejectedBlob.exists()) {
            // next steps will overwrite the file, create a snapshot of current version
            rejectedBlob.createSnapshot();
        }
        rejectedBlob.startCopy(inputBlob);

        waitUntilBlobIsCopied(rejectedBlob);

        AccessCondition deleteCondition = leaseId != null
            ? AccessCondition.generateLeaseCondition(leaseId)
            : AccessCondition.generateEmptyCondition();

        try {
            inputBlob.deleteIfExists(DeleteSnapshotsOption.NONE, deleteCondition, null, null);
        } catch (StorageException e) {
            //if lease lost retry
            log.warn(
                "Deleting File {} got error, Error code {}, Http status {} ",
                fileName,
                e.getErrorCode(),
                e.getHttpStatusCode(),
                e
            );

            if (e.getHttpStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED
                && StorageErrorCodeStrings.LEASE_LOST.equals(e.getErrorCode())) {
                log.info("Deleting File {} got error, retrying...", fileName);
                inputBlob.deleteIfExists(DeleteSnapshotsOption.NONE, null, null, null);
            } else {
                throw e;
            }
        }
        log.info("File {} moved to rejected container {}", fileName, rejectedContainerName);
    }

    private CloudBlockBlob getBlob(String fileName, String inputContainerName)
        throws URISyntaxException, StorageException {

        CloudBlobContainer inputContainer = cloudBlobClient.getContainerReference(inputContainerName);
        return inputContainer.getBlockBlobReference(fileName);
    }

    public void newTryMoveFileToRejectedContainer(String fileName, String inputContainerName, String leaseId) {
        String rejectedContainerName = getRejectedContainerName(inputContainerName);

        try {
            newMoveFileToRejectedContainer(
                fileName,
                inputContainerName,
                rejectedContainerName,
                leaseId
            );
        } catch (Exception ex) {
            log.error(
                "An error occurred when moving rejected file {} from container {} to rejected files' container {}",
                fileName,
                inputContainerName,
                rejectedContainerName,
                ex
            );
        }
    }

    private void newMoveFileToRejectedContainer(
        String fileName,
        String inputContainerName,
        String rejectedContainerName,
        String leaseId
    ) {
        log.info(
            "Moving rejected file {} from container {} to {}",
            fileName,
            inputContainerName,
            rejectedContainerName
        );

        BlobClient inputBlob = blobServiceClient
            .getBlobContainerClient(inputContainerName)
            .getBlobClient(fileName);

        BlobClient rejectedBlob = blobServiceClient
            .getBlobContainerClient(rejectedContainerName)
            .getBlobClient(fileName);

        if (rejectedBlob.exists()) {
            // next steps will overwrite the file, create a snapshot of current version
            rejectedBlob.createSnapshot();
        }
        String copyId = rejectedBlob.copyFromUrl(inputBlob.getBlobUrl());

        log.info("Rejected file copied to rejected container: {}, copyId: {}",
            rejectedContainerName, copyId);

        try {
            inputBlob.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                new BlobRequestConditions().setLeaseId(leaseId),
                null,
                Context.NONE
            );
        } catch (BlobStorageException e) {
            //if lease lost retry
            log.warn(
                "Deleting File {} got error, Error code {}, Http status {} ",
                fileName,
                e.getErrorCode(),
                e.getStatusCode(),
                e
            );

            if (e.getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED
                && BlobErrorCode.LEASE_LOST.equals(e.getErrorCode())) {
                log.info("Deleting File {} got error, retrying...", fileName);
                inputBlob.deleteWithResponse(
                    DeleteSnapshotsOptionType.ONLY,
                    null,
                    null,
                    Context.NONE
                );
            } else {
                throw e;
            }
        }
        log.info("File {} moved to rejected container {}", fileName, rejectedContainerName);
    }

    private void waitUntilBlobIsCopied(CloudBlockBlob blob) throws StorageException {

        CopyStatus copyStatus = CopyStatus.PENDING;
        boolean timeout = false;
        LocalDateTime startTime = LocalDateTime.now();

        do {
            if (LocalDateTime.now().minus(properties.getBlobCopyTimeoutInMillis(), MILLIS).isAfter(startTime)) {
                timeout = true;
            } else {
                Uninterruptibles.sleepUninterruptibly(properties.getBlobCopyPollingDelayInMillis(), MILLISECONDS);
                blob.downloadAttributes();
                copyStatus = blob.getCopyState().getStatus();
            }
        } while (copyStatus == CopyStatus.PENDING && !timeout);

        if (copyStatus != CopyStatus.SUCCESS) {
            String errorMessage = timeout
                ? "Timed out while waiting for rejected file to be copied"
                : String.format("Unexpected status of copy operation: %s", copyStatus);

            throw new RejectedBlobCopyException(errorMessage);
        }
    }

    private String getRejectedContainerName(String inputContainerName) {
        return inputContainerName + REJECTED_CONTAINER_NAME_SUFFIX;
    }

    private void logAcquireLeaseError(String zipFilename, String containerName, Exception exception) {
        log.error(
            "Failed to acquire lease on file {} in container {}",
            zipFilename,
            containerName,
            exception
        );
    }


    private boolean readyToAcquireLease(CloudBlockBlob cloudBlockBlob, String fileName, String container) {
        // TODO: remove filename and container params and logging when the lease issue is resolved
        String leaseExpirationTime = cloudBlockBlob.getMetadata().get(LEASE_EXPIRATION_TIME);
        log.info(
            "Checking if lease acquired on file {} in container {}. Lease Expiration Time: {}",
            fileName,
            container,
            leaseExpirationTime
        );

        if (StringUtils.isBlank(leaseExpirationTime)) {
            return true; // lease not acquired on file
        } else {
            LocalDateTime leaseExpiresAt = LocalDateTime.parse(leaseExpirationTime);
            return leaseExpiresAt.isBefore(LocalDateTime.now(EUROPE_LONDON_ZONE_ID)); // check if lease expired
        }

    }
}
