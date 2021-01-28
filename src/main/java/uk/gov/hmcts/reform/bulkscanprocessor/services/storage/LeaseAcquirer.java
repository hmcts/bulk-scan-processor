package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.LEASE_ALREADY_PRESENT;
import static com.azure.storage.blob.models.CopyStatusType.PENDING;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker.LEASE_EXPIRATION_TIME;

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
     *
     * @param blobClient Represents blob
     * @param onLeaseSuccess Consumer which takes in {@code leaseId} acquired with {@link BlobLeaseClient}
     * @param onFailure Extra step to execute in case an error occurred
     * @param releaseLease Flag whether to release the lease or not
     */
    public void ifAcquiredOrElse(
        BlobClient blobClient,
        Consumer<String> onLeaseSuccess,
        Consumer<BlobErrorCode> onFailure,
        boolean releaseLease
    ) {
        try {

            if (PENDING == blobClient.getProperties().getCopyStatus()) {
                logger.warn(
                    "Copy in progress skipping , file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
                return;
            }
            var leaseClient = leaseClientProvider.get(blobClient);
            var leaseId = leaseClient.acquireLease(LEASE_DURATION_IN_SECONDS);
            boolean isReady = false;

            try {
                isReady = leaseMetaDataChecker.isReadyToUse(blobClient, leaseId);
            } catch (Exception ex) {
                logger.warn(
                    "Could not check meta data for lease expiration on file {} in container {}",
                    blobClient.getBlobName(),
                    blobClient.getContainerName()
                );
            } finally {
                if (!isReady) {
                    release(leaseClient, blobClient);
                    //it means lease did not acquired let the failure function decide
                    onFailure.accept(LEASE_ALREADY_PRESENT);
                }
            }

            if (isReady) {
                onLeaseSuccess.accept(leaseId);
                if (releaseLease) {
                    clearMetadataAndReleaseLease(leaseClient, blobClient, leaseId);
                }
            }
        } catch (BlobStorageException exc) {

            String logContext = "Error acquiring lease for blob. "
                + "File name: " + blobClient.getBlobName()
                + ", Container: " + blobClient.getContainerName();

            if (exc.getErrorCode() != LEASE_ALREADY_PRESENT && exc.getErrorCode() != BLOB_NOT_FOUND) {
                logger.error(logContext, exc);
            } else {
                logger.info(logContext, exc);
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

    private void clearMetadataAndReleaseLease(
        BlobLeaseClient leaseClient,
        BlobClient blobClient,
        String leaseId
    ) {
        try {
            Map<String, String> blobMetaData = blobClient.getProperties().getMetadata();
            blobMetaData.remove(LEASE_EXPIRATION_TIME);
            blobClient.setMetadataWithResponse(
                blobMetaData,
                new BlobRequestConditions().setLeaseId(leaseId),
                null,
                Context.NONE
            );
            release(leaseClient, blobClient);
        } catch (BlobStorageException exc) {
            logger.warn(
                "Could not clear metadata, lease with ID {}. Blob: {}, container: {}",
                leaseClient.getLeaseId(),
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                exc
            );
        }
    }

}
