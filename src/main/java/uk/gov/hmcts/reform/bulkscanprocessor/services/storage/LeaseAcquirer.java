package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class LeaseAcquirer {

    public static final int LEASE_DURATION_IN_SECONDS = 60;
    private static final Logger logger = getLogger(LeaseAcquirer.class);

    private final LeaseClientProvider leaseClientProvider;
    private final LeaseMetaDataChecker leaseMetaDataChecker;

    public LeaseAcquirer(
        LeaseClientProvider leaseClientProvider,
        LeaseMetaDataChecker leaseMetaDataChecker
    ) {
        this.leaseClientProvider = leaseClientProvider;
        this.leaseMetaDataChecker = leaseMetaDataChecker;
    }

    /**
     * Main wrapper for blobs to be leased by {@link BlobLeaseClient}.
     * @param blobClient Represents blob
     * @param onLeaseSuccess Consumer which takes in {@code leaseId} acquired with {@link BlobLeaseClient}
     * @param onFailure Extra step to execute in case an error occurred
     * @param releaseLease Flag weather to release the lease or not
     */
    public void ifAcquiredOrElse(
        BlobClient blobClient,
        Consumer<String> onLeaseSuccess,
        Consumer<BlobErrorCode> onFailure,
        boolean releaseLease
    ) {
        try {
            var leaseClient = leaseClientProvider.get(blobClient);
            var leaseId = leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);

            boolean isReady = leaseMetaDataChecker.isReadyToUse(blobClient, leaseId);

            if (!isReady) {
                release(leaseClient, blobClient);
            } else {
                onLeaseSuccess.accept(leaseId);
                if (releaseLease) {
                    release(leaseClient, blobClient);
                }
            }
        } catch (BlobStorageException exc) {
            if (exc.getErrorCode() != LEASE_ALREADY_PRESENT && exc.getErrorCode() != BLOB_NOT_FOUND) {
                logger.error(
                    "Error acquiring lease for blob. File name: {}, Container: {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName(),
                    exc
                );
            }

            onFailure.accept(exc.getErrorCode());
        }
    }

    private void release(BlobLeaseClient leaseClient, BlobClient blobClient) {
        try {
            leaseClient.releaseLease();
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not release the lease with ID {}. Blob: {}, container: {}",
                leaseClient.getLeaseId(),
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }

}
