package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;

import java.util.List;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;

/**
 * Service to delete files from the blob storage.
 */
@Service
public class DeleteFilesService {
    private static final Logger log = LoggerFactory.getLogger(DeleteFilesService.class);

    private final EnvelopeRepository envelopeRepository;

    private final EnvelopeMarkAsDeletedService envelopeMarkAsDeletedService;

    private final LeaseAcquirer leaseAcquirer;

    /**
     * Constructor for DeleteFilesService
     * @param envelopeRepository EnvelopeRepository
     * @param envelopeMarkAsDeletedService EnvelopeMarkAsDeletedService
     * @param leaseAcquirer LeaseAcquirer
     */
    public DeleteFilesService(
        EnvelopeRepository envelopeRepository,
        EnvelopeMarkAsDeletedService envelopeMarkAsDeletedService,
        LeaseAcquirer leaseAcquirer
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeMarkAsDeletedService = envelopeMarkAsDeletedService;
        this.leaseAcquirer = leaseAcquirer;
    }

    /**
     * Deletes complete files from the given container.
     * @param container BlobContainerClient
     */
    public void processCompleteFiles(BlobContainerClient container) {
        log.info("Started deleting complete files in container {}", container.getBlobContainerName());

        List<Envelope> envelopes =
            envelopeRepository.getCompleteEnvelopesFromContainer(
                container.getBlobContainerName()
            );
        int successCount = 0;
        int failureCount = 0;
        for (Envelope envelope : envelopes) {
            if (tryProcessCompleteEnvelope(container, envelope)) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        log.info(
            "Finished deleting complete files in container {}, deleted {} files, failed to delete {} files",
            container.getBlobContainerName(),
            successCount,
            failureCount
        );
    }

    /**
     * Tries to process the given envelope.
     * @param container BlobContainerClient
     * @param envelope Envelope
     * @return true if the envelope was processed successfully, false otherwise
     */
    private boolean tryProcessCompleteEnvelope(BlobContainerClient container, Envelope envelope) {

        String loggingContext = "File name: " + envelope.getZipFileName()
            + ", Container: " + container.getBlobContainerName();

        try {
            log.info("Deleting file. {}", loggingContext);

            BlobClient blobClient = container.getBlobClient(envelope.getZipFileName());

            if (Boolean.TRUE.equals(blobClient.exists())) {

                leaseAcquirer.ifAcquiredOrElse(
                    blobClient,
                    leaseId -> deleteBlob(blobClient),
                    //should throw this if deletion or lease acquiring fails envelope should not be marked as deleted
                    errorCode -> throwBlobDeleteException(errorCode, loggingContext),
                    false
                );

            } else {
                log.info("File has already been deleted. {}", loggingContext);
            }


            envelopeMarkAsDeletedService.markEnvelopeAsDeleted(envelope.getId(), loggingContext);
            log.info("Delete processes completed.  Envelope marked as deleted. {}", loggingContext);

            return true;

        } catch (Exception ex) {
            log.error("Failed to process file. {}", loggingContext, ex);
            return false;
        }
    }

    /**
     * Deletes the blob.
     * @param blobClient BlobClient
     */
    private void deleteBlob(BlobClient blobClient) {
        blobClient.deleteWithResponse(
            DeleteSnapshotsOptionType.INCLUDE,
            null,
            null,
            Context.NONE
        );

        log.info("Blob {}  is deleted", blobClient.getBlobUrl());
    }

    /**
     * Throws BlobDeleteException.
     * @param errorCode BlobErrorCode
     * @param loggingContext String
     */
    private void throwBlobDeleteException(BlobErrorCode errorCode, String loggingContext) {
        if (BLOB_NOT_FOUND == errorCode) {
            log.info(
                "Blob not found. Envelope should mark as deleted. {}",
                loggingContext
            );
        } else {
            throw new BlobDeleteException("Deleting blob failed. ErrorCode: " + errorCode.toString());
        }
    }
}
