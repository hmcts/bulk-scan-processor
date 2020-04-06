package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;

@Service
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

    private void processCompleteFiles(CloudBlobContainer container) {
        log.info("Started deleting complete files in container {}", container.getName());

        List<Envelope> envelopes = envelopeRepository.findByContainerAndStatusAndZipDeleted(
            container.getName(),
            COMPLETED,
            false
        );
        int successCount = 0;
        int failureCount = 0;
        for (Envelope envelope: envelopes) {
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

        try {
            CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(envelope.getZipFileName());

            log.info(
                "File {} (container {}) is complete - deleting",
                envelope.getZipFileName(),
                container.getName()
            );

            if (cloudBlockBlob.exists()) {
                deleted = cloudBlockBlob.deleteIfExists();
                if (deleted) {
                    log.info(
                        "Deleted file {} from container {}",
                        envelope.getZipFileName(),
                        container.getName()
                    );
                } else {
                    log.info(
                        "File {} has not been deleted from container {}",
                        envelope.getZipFileName(),
                        container.getName()
                    );
                }
            } else {
                deleted = true;
                log.info(
                    "File {} (container {}) has already been deleted.",
                    envelope.getZipFileName(),
                    container.getName()
                );
            }
            if (deleted) {
                envelope.setZipDeleted(true);
                envelopeRepository.saveAndFlush(envelope);
                log.info(
                    "Marked envelope from file {} (container {}) as deleted",
                    envelope.getZipFileName(),
                    container.getName()
                );
            }
        } catch (Exception ex) {
            log.error(
                "Failed to process file {} from container {}",
                envelope.getZipFileName(),
                container.getName(),
                ex
            );
        }

        return deleted;
    }
}
