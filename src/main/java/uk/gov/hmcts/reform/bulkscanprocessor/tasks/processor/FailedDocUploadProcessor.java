package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;

import java.util.List;

@Component
public class FailedDocUploadProcessor {

    private static final Logger log = LoggerFactory.getLogger(FailedDocUploadProcessor.class);

    private final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    private final EnvelopeProcessor envelopeProcessor;
    private final ErrorHandlingWrapper errorWrapper;

    public FailedDocUploadProcessor(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.errorWrapper = errorWrapper;
    }

    public void processJurisdiction(String jurisdiction) {
        List<Envelope> envelopes = envelopeProcessor.getFailedToUploadEnvelopes(jurisdiction);

        if (envelopes.size() > 0) {
            String containerName = envelopes.get(0).getContainer();

            processEnvelopes(containerName, envelopes);
        }
    }

    private void processEnvelopes(String containerName, List<Envelope> envelopes) {
        log.info("Processing {} failed documents for container {}", envelopes.size(), containerName);
    }
}
