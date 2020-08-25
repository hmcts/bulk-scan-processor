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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobCopyException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);
    private static final String REJECTED_CONTAINER_NAME_SUFFIX = "-rejected";
    private static final String SELECT_ALL_CONTAINER = "ALL";

    private final BlobServiceClient blobServiceClient;

    private final BlobManagementProperties properties;

    public BlobManager(
        BlobServiceClient blobServiceClient,
        BlobManagementProperties properties
    ) {
        this.blobServiceClient = blobServiceClient;
        this.properties = properties;
    }

    public BlobContainerClient getContainerClient(String containerName) {
        return blobServiceClient.getBlobContainerClient(containerName);
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
        log.info("Moving file {} from container {} to {}", fileName, inputContainerName, rejectedContainerName);
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

        // Blob Api provides `copyFromUrl` but it did not work with bulk-scan staging app gateway.
        copyBlob(inputBlob, rejectedBlob);

        log.info("Rejected file copied to rejected container: {} ", rejectedContainerName);

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

    private void copyBlob(BlobClient sourceBlobClient, BlobClient targetBlobClient) {

        String sourceBlobUrl = sourceBlobClient.getBlobUrl();
        String targetBlobUrl = targetBlobClient.getBlobUrl();
        log.info("Copying from {} to {}", sourceBlobUrl, targetBlobUrl);
        byte[] rawBlob;
        try (var outputStream = new ByteArrayOutputStream()) {
            sourceBlobClient.download(outputStream);
            rawBlob = outputStream.toByteArray();
        } catch (Exception e) {
            throw new BlobCopyException(
                "Copy blob failed in download, Downloading from : " + sourceBlobUrl,
                e
            );
        }

        try (var uploadContent = new ByteArrayInputStream(rawBlob)) {
            targetBlobClient.upload(uploadContent, rawBlob.length, true);
        } catch (Exception ex) {
            throw new BlobCopyException(
                "Copy blob failed in Upload, Upload  target: " + targetBlobUrl,
                ex
            );
        }
    }

    private String getRejectedContainerName(String inputContainerName) {
        return inputContainerName + REJECTED_CONTAINER_NAME_SUFFIX;
    }

}
