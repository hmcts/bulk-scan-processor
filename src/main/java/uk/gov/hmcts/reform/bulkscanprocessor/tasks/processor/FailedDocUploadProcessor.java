package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.Processor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.zip.ZipInputStream;

@Component
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, // every other call to return bean will create new instance
    proxyMode = ScopedProxyMode.TARGET_CLASS // allows prototyping
)
public class FailedDocUploadProcessor extends Processor {

    private static final Logger log = LoggerFactory.getLogger(FailedDocUploadProcessor.class);

    private final ZipFileProcessor zipFileProcessor;

    @Autowired
    public FailedDocUploadProcessor(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ZipFileProcessor zipFileProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository
    ) {
        super(blobManager, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
        this.zipFileProcessor = zipFileProcessor;
    }

    public void processJurisdiction(String jurisdiction) {
        log.info("Started looking for failed documents for jurisdiction {}", jurisdiction);

        try {
            List<Envelope> envelopes = envelopeProcessor.getFailedToUploadEnvelopes(jurisdiction);

            if (!envelopes.isEmpty()) {
                String containerName = envelopes.get(0).getContainer();

                processEnvelopes(containerName, envelopes);
            }

            log.info(
                "Finished processing failed documents for jurisdiction {}. Processed {} envelopes.",
                jurisdiction,
                envelopes.size()
            );
        } catch (Exception ex) {
            log.error("An error occurred when processing failed documents for jurisdiction {}.", jurisdiction, ex);
        }
    }

    private void processEnvelopes(String containerName, List<Envelope> envelopes)
        throws StorageException, URISyntaxException {

        log.info("Processing {} failed documents for container {}", envelopes.size(), containerName);

        CloudBlobContainer container = blobManager.getContainer(containerName);

        for (Envelope envelope : envelopes) {
            try {
                processEnvelope(container, envelope);
            } catch (Exception ex) {
                log.error(
                    "An error occurred when processing failed document {} from container {}. Envelope ID: {}",
                    envelope.getZipFileName(),
                    containerName,
                    envelope.getId(),
                    ex
                );
            }
        }
    }

    private void processEnvelope(CloudBlobContainer container, Envelope envelope)
        throws IOException, StorageException, URISyntaxException {

        log.info("Processing zip file {} from container {}", envelope.getZipFileName(), envelope.getContainer());

        BlobInputStream blobInputStream = container
            .getBlockBlobReference(envelope.getZipFileName())
            .openInputStream();

        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            ZipFileProcessingResult result = processZipInputStream(
                zis,
                container.getName(),
                envelope.getZipFileName()
            );

            if (result != null) {
                processParsedEnvelopeDocuments(
                    envelope,
                    result.getPdfs()
                );
            }
        }

        log.info("Processed zip file {} from container {}", envelope.getZipFileName(), envelope.getContainer());
    }

    private ZipFileProcessingResult processZipInputStream(
        ZipInputStream zis,
        String containerName,
        String zipFileName
    ) {
        try {
            return zipFileProcessor.process(zis, zipFileName);
        } catch (Exception ex) {
            log.error("Failed to reprocess file {} from container {}", zipFileName, containerName, ex);
            handleEventRelatedError(Event.DOC_FAILURE, containerName, zipFileName, ex);
            return null;
        }
    }
}
