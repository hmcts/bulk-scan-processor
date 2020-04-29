package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.BLOB_DELETE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Service
@ConditionalOnProperty(value = "scheduling.task.delete-complete-files.enabled")
public class DeleteCompleteFilesTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteCompleteFilesTask.class);

    private static final String TASK_NAME = "delete-complete-files";

    private final BlobManager blobManager;
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository eventRepository;

    public DeleteCompleteFilesTask(
        BlobManager blobManager,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository
    ) {
        this.blobManager = blobManager;
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "${scheduling.task.delete-complete-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "delete-complete-files")
    public void run() {
        log.info("Started {} task", TASK_NAME);

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

        log.info("Finished {} task", TASK_NAME);
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
        boolean deleted;

        String loggingContext = "File name: " + envelope.getZipFileName() + ", Container: " + container.getName();

        try {
            log.info("Deleting file. " + loggingContext);

            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(envelope.getZipFileName());

            if (cloudBlockBlob.exists()) {
                deleted = cloudBlockBlob.deleteIfExists();
                if (deleted) {
                    log.info("File deleted. {}", loggingContext);
                } else {
                    log.info("File has not been deleted. {}", loggingContext);
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
            registerDeleteFailureEvent(envelope, ex.getMessage());
            return false;
        }
    }

    private void registerDeleteFailureEvent(
        Envelope envelope,
        String reason
    ) {
        ProcessEvent processEvent = new ProcessEvent(
            envelope.getContainer(),
            envelope.getZipFileName(),
            BLOB_DELETE_FAILURE
        );

        processEvent.setReason(reason);
        eventRepository.saveAndFlush(processEvent);

        log.info(
            "Zip {} from {} marked as {}",
            processEvent.getZipFileName(),
            processEvent.getContainer(),
            processEvent.getEvent()
        );
    }
}
