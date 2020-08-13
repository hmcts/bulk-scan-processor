package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Service
@ConditionalOnProperty(value = "scheduling.task.delete-complete-files.enabled")
public class DeleteCompleteFilesTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteCompleteFilesTask.class);

    private static final String TASK_NAME = "delete-complete-files";

    private final BlobManager blobManager;
    private final EnvelopeRepository envelopeRepository;
    private final LeaseAcquirer leaseAcquirer;

    public DeleteCompleteFilesTask(
        BlobManager blobManager,
        EnvelopeRepository envelopeRepository,
        LeaseAcquirer leaseAcquirer
    ) {
        this.blobManager = blobManager;
        this.envelopeRepository = envelopeRepository;
        this.leaseAcquirer = leaseAcquirer;
    }

    @Scheduled(cron = "${scheduling.task.delete-complete-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        log.info("Started {} job", TASK_NAME);

        for (BlobContainerClient container : blobManager.getInputContainerClients()) {
            try {
                processCompleteFiles(container);
            } catch (Exception ex) {
                log.error(
                    "Failed to process files from container {}",
                    container.getBlobContainerName(),
                    ex
                );
            }
        }

        log.info("Finished {} job", TASK_NAME);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void processCompleteFiles(BlobContainerClient container) {
        log.info("Started deleting complete files in container {}", container.getBlobContainerName());

        List<Envelope> envelopes = envelopeRepository.findByContainerAndStatusAndZipDeleted(
            container.getBlobContainerName(),
            COMPLETED,
            false
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

            if (Boolean.TRUE == blobClient.exists()) {

                leaseAcquirer.ifAcquiredOrElse(
                    blobClient,
                    leaseId -> delete(blobClient, leaseId),
                    DeleteCompleteFilesTask::throwBlobDeleteException,
                    false
                );

            } else {
                log.info("File has already been deleted. {}", loggingContext);
            }

            envelope.setZipDeleted(true);
            envelopeRepository.saveAndFlush(envelope);
            log.info("Marked envelope as deleted. {}", loggingContext);

            return true;

        } catch (Exception ex) {
            log.error("Failed to process file. {}", loggingContext, ex);
            return false;
        }
    }

    private void delete(BlobClient blobClient, String leaseId) {
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
