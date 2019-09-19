package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.google.common.util.concurrent.Uninterruptibles;
import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.DeleteSnapshotsOption;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.RejectedBlobCopyException;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);
    private static final String REJECTED_CONTAINER_NAME_SUFFIX = "-rejected";
    private static final String SELECT_ALL_CONTAINER = "ALL";
    private static final String LEASE_ALREADY_ACQUIRED_MESSAGE =
        "Can't acquire lease on file {} in container {} - already acquired";

    private final CloudBlobClient cloudBlobClient;
    private final BlobManagementProperties properties;

    public BlobManager(
        CloudBlobClient cloudBlobClient,
        BlobManagementProperties properties
    ) {
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
            }

            String leaseId = cloudBlockBlob.acquireLease(properties.getBlobLeaseTimeout(), null);
            log.info("Acquired lease on file {} in container {}", zipFilename, containerName);
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
            throw new ContainerNotFoundException();
        }

        return cloudBlobContainerList;
    }

    private boolean filterBySelectedContainer(CloudBlobContainer container) {
        return SELECT_ALL_CONTAINER.equalsIgnoreCase(properties.getBlobSelectedContainer())
            || properties.getBlobSelectedContainer().equals(container.getName());
    }

    public List<CloudBlobContainer> listRejectedContainers() {
        return StreamSupport
            .stream(cloudBlobClient.listContainers().spliterator(), false)
            .filter(c -> c.getName().endsWith(REJECTED_CONTAINER_NAME_SUFFIX))
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

        inputBlob.deleteIfExists(DeleteSnapshotsOption.NONE, deleteCondition, null, null);
        log.info("File {} moved to rejected container {}", fileName, rejectedContainerName);
    }

    private CloudBlockBlob getBlob(String fileName, String inputContainerName)
        throws URISyntaxException, StorageException {

        CloudBlobContainer inputContainer = cloudBlobClient.getContainerReference(inputContainerName);
        return inputContainer.getBlockBlobReference(fileName);
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
}
