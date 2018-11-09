package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

@Component
public class BlobManager {

    private static final Logger log = LoggerFactory.getLogger(BlobManager.class);

    @Value("${storage.blob_lease_timeout}")
    private Integer blobLeaseTimeout;

    private final CloudBlobClient cloudBlobClient;

    public BlobManager(CloudBlobClient cloudBlobClient) {
        this.cloudBlobClient = cloudBlobClient;
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

            cloudBlockBlob.acquireLease(blobLeaseTimeout, null);
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
}
