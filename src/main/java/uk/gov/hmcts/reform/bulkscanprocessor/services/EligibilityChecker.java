package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class EligibilityChecker {

    private static final Logger log = LoggerFactory.getLogger(EligibilityChecker.class);
    private final EnvelopeProcessor envelopeProcessor;

    public EligibilityChecker(EnvelopeProcessor envelopeProcessor) {
        this.envelopeProcessor = envelopeProcessor;
    }

    public boolean isEligibleForProcessing(
        CloudBlockBlob cloudBlockBlob,
        String containerName,
        String zipFilename
    ) throws StorageException {
        Envelope existingEnvelope =
                envelopeProcessor.getEnvelopeByFileAndContainer(containerName, zipFilename);

        if (existingEnvelope != null) {
            log.warn(
                "Envelope for zip file {} (container {}) already exists. Aborting its processing. Envelope ID: {}",
                zipFilename,
                containerName,
                existingEnvelope.getId()
            );
            return false;
        } else if (!cloudBlockBlob.exists()) {
            log.info(
                "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
                zipFilename,
                containerName
            );
            return false;
        } else {
           return true;
        }
    }
}
