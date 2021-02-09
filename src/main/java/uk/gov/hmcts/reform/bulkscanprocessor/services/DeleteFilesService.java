package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeJdbcRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;

import java.util.List;

@Service
public class DeleteFilesService {
    private static final Logger log = LoggerFactory.getLogger(DeleteFilesService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeJdbcRepository envelopeJdbcRepository;

    private final LeaseAcquirer leaseAcquirer;

    public DeleteFilesService(
        EnvelopeRepository envelopeRepository,
        EnvelopeJdbcRepository envelopeJdbcRepository,
        LeaseAcquirer leaseAcquirer
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeJdbcRepository = envelopeJdbcRepository;
        this.leaseAcquirer = leaseAcquirer;
    }

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

    private boolean tryProcessCompleteEnvelope(BlobContainerClient container, Envelope envelope) {

        String loggingContext = "File name: " + envelope.getZipFileName()
            + ", Container: " + container.getBlobContainerName();

        try {
            log.info("Deleting file. {}", loggingContext);

            BlobClient blobClient = container.getBlobClient(envelope.getZipFileName());

            if (Boolean.TRUE.equals(blobClient.exists())) {

                leaseAcquirer.ifAcquiredOrElse(
                    blobClient,
                    leaseId -> deleteBlob(blobClient, leaseId),
                    //should throw this if deletion or lease acquiring fails envelope should not be marked as deleted
                    DeleteFilesService::throwBlobDeleteException,
                    false
                );

            } else {
                log.info("File has already been deleted. {}", loggingContext);
            }

            envelopeJdbcRepository.markEnvelopeAsDeleted(envelope.getId());
            log.info("Marked envelope as deleted. {}", loggingContext);

            return true;

        } catch (Exception ex) {
            log.error("Failed to process file. {}", loggingContext, ex);
            return false;
        }
    }

    private void deleteBlob(BlobClient blobClient, String leaseId) {
        blobClient.deleteWithResponse(
            DeleteSnapshotsOptionType.INCLUDE,
            new BlobRequestConditions().setLeaseId(leaseId),
            null,
            Context.NONE
        );

        log.info(
            "Deleted completed file {} from container {}",
            blobClient.getBlobName(),
            blobClient.getContainerName()
        );
    }

    private static void throwBlobDeleteException(BlobErrorCode errorCode) {
        throw new BlobDeleteException("Deleting blob failed. ErrorCode: " + errorCode.toString());
    }
}
