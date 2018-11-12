package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.RejectedBlobCopyException;

import java.net.URISyntaxException;

@Component
@EnableConfigurationProperties(BlobManagementProperties.class)
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);

    private final CloudBlobClient cloudBlobClient;
    private final BlobManagementProperties properties;

    public BlobManager(
        CloudBlobClient cloudBlobClient,
        BlobManagementProperties properties
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.properties = properties;
    }

    public boolean acquireLease(CloudBlockBlob cloudBlockBlob, String containerName, String zipFilename) {
        try {
            // Note: trying to lease an already leased blob throws an exception and
            // we really do not want to fill the application logs with these. Unfortunately
            // even with this check there is still a chance of an exception as check + lease
            // cannot be expressed as an atomic operation (not that I can see anyway).
            // All considered this should still be much better than not checking lease status
            // at all.
            if (cloudBlockBlob.getProperties().getLeaseStatus() == LeaseStatus.LOCKED) {
                log.debug("Lease already acquired for container {} and zip file {}",
                    containerName, zipFilename);
                return false;
            }

            cloudBlockBlob.acquireLease(properties.getBlobLeaseTimeout(), null);
            return true;
        } catch (StorageException storageException) {
            if (storageException.getHttpStatusCode() == HttpStatus.CONFLICT.value()) {
                log.error(
                    "Lease already acquired for container {} and zip file {}",
                    containerName,
                    zipFilename,
                    storageException
                );
            } else {
                log.error(storageException.getMessage(), storageException);
            }
            return false;
        } catch (Exception exception) {
            log.error(
                "Failed to acquire lease on file {} in container {}",
                zipFilename,
                containerName,
                exception
            );

            return false;
        }
    }

    public CloudBlobContainer getContainer(String containerName) throws URISyntaxException, StorageException {
        return cloudBlobClient.getContainerReference(containerName);
    }

    public Iterable<CloudBlobContainer> listContainers() {
        return cloudBlobClient.listContainers();
    }

    public void tryMoveFileToRejectedContainer(String fileName, String inputContainerName) {
        try {
            moveFileToRejectedContainer(
                fileName,
                inputContainerName,
                getRejectedContainerName(inputContainerName)
            );
        } catch (Exception ex) {
            log.error(
                "An error occurred when moving rejected file {} from container {} to rejected files' container {}",
                fileName,
                inputContainerName,
                getRejectedContainerName(inputContainerName),
                ex
            );
        }
    }

    private void moveFileToRejectedContainer(String fileName, String inputContainerName, String rejectedContainerName)
        throws URISyntaxException, StorageException, InterruptedException {

        CloudBlobContainer inputContainer = cloudBlobClient.getContainerReference(inputContainerName);
        CloudBlobContainer rejectedContainer = cloudBlobClient.getContainerReference(rejectedContainerName);
        CloudBlockBlob inputBlob = inputContainer.getBlockBlobReference(fileName);
        CloudBlockBlob rejectedBlob = rejectedContainer.getBlockBlobReference(fileName);
        rejectedBlob.startCopy(inputBlob);

        waitUntilBlobIsCopied(rejectedBlob);
        inputBlob.deleteIfExists();
        log.info("File {} moved to rejected container {}", fileName, rejectedContainer.getName());
    }

    private void waitUntilBlobIsCopied(CloudBlockBlob blob)
        throws InterruptedException, StorageException {

        CopyStatus copyStatus = CopyStatus.PENDING;
        boolean timeout = false;
        DateTime startTime = DateTime.now();

        do {
            if (DateTime.now().minusMillis(properties.getBlobCopyTimeoutInMillis()).isAfter(startTime)) {
                timeout = true;
            } else {
                Thread.sleep(properties.getBlobCopyPollingDelayInMillis());
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
        return inputContainerName + "-rejected";
    }
}
