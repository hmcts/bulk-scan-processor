package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.Processor;

import java.util.List;
import java.util.zip.ZipInputStream;

@Component
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, // every other call to return bean will create new instance
    proxyMode = ScopedProxyMode.TARGET_CLASS // allows prototyping
)
public class FailedDocUploadProcessor extends Processor {

    private static final Logger log = LoggerFactory.getLogger(FailedDocUploadProcessor.class);

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

    private void processZipFile(CloudBlobContainer container, Envelope envelope) {
    }

    private ZipFileProcessor processZipInputStream(ZipInputStream zis, Envelope envelope) {
        return null;
    }
}
