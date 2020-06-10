package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.storage.AccessCondition.generateLeaseCondition;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.NONE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Service
@ConditionalOnProperty(value = "scheduling.task.delete-complete-files.enabled")
public class DeleteCompleteFilesTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteCompleteFilesTask.class);

    private static final String TASK_NAME = "delete-complete-files";

    private final BlobManager blobManager;
    private final EnvelopeRepository envelopeRepository;

    public DeleteCompleteFilesTask(
        BlobManager blobManager,
        EnvelopeRepository envelopeRepository
    ) {
        this.blobManager = blobManager;
        this.envelopeRepository = envelopeRepository;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.delete-complete-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        log.info("Started {} job", TASK_NAME);

        for (CloudBlobContainer container : blobManager.listInputContainers()) {
            try {
                processCompleteFiles(container);
            } catch (Exception ex) {
                log.error(
                    "Failed to process files from container {}",
                    container.getName(),
                    ex
                );
            }
        }

        log.info("Finished {} job", TASK_NAME);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void processCompleteFiles(CloudBlobContainer container) {
        log.info("Started deleting complete files in container {}", container.getName());

        List<Envelope> envelopes = envelopeRepository.findByContainerAndStatusAndZipDeleted(
            container.getName(),
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
            container.getName(),
            successCount,
            failureCount
        );
    }

    private boolean tryProcessCompleteEnvelope(CloudBlobContainer container, Envelope envelope) {
        boolean deleted = false;

        String loggingContext = "File name: " + envelope.getZipFileName() + ", Container: " + container.getName();

        try {
            log.info("Deleting file. {}", loggingContext);

            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(envelope.getZipFileName());

            if (cloudBlockBlob.exists()) {
                Optional<String> leaseId = blobManager.acquireLease(
                    cloudBlockBlob,
                    container.getName(),
                    envelope.getZipFileName()
                );
                if (leaseId.isPresent()) {
                    AccessCondition accessCondition = generateLeaseCondition(leaseId.get());
                    deleted = cloudBlockBlob.deleteIfExists(
                        NONE,
                        accessCondition,
                        null,
                        null
                    );
                    if (deleted) {
                        log.info("File deleted. {}", loggingContext);
                    } else {
                        log.info("File has not been deleted. {}", loggingContext);
                    }
                }
            } else {
                deleted = true;
                log.info("File has already been deleted. {}", loggingContext);
            }

            if (deleted) {
                envelope.setZipDeleted(true);
                envelopeRepository.saveAndFlush(envelope);
                log.info("Marked envelope as deleted. {}", loggingContext);
            }

            return deleted;

        } catch (Exception ex) {
            log.error("Failed to process file. {}", loggingContext, ex);
            return false;
        }
    }
}
