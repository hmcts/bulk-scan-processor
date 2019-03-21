package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
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

    @Autowired
    public FailedDocUploadProcessor(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        ContainerMappings containerMappings
    ) {
        super(blobManager, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
    }

    // NOTE: this is needed for testing as children of this class are instantiated
    // using "new" in tests despite being spring beans (sigh!)
    @SuppressWarnings("squid:S00107")
    public FailedDocUploadProcessor(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        ContainerMappings containerMappings,
        ServiceBusHelper serviceBusHelper, // NOSONAR
        String signatureAlg,
        String publicKeyDerFilename
    ) {
        this(blobManager, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository, containerMappings);
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    public void processJurisdiction(String jurisdiction)
        throws IOException, StorageException, URISyntaxException {
        log.info("Started processing failed documents for jurisdiction {}", jurisdiction);

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
        throws IOException, StorageException, URISyntaxException {

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

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(envelope.getZipFileName());
        BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();

        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            ZipFileProcessingResult result = processZipInputStream(
                zis,
                container.getName(),
                envelope.getZipFileName()
            );

            if (result != null) {
                processParsedEnvelopeDocuments(
                    envelope,
                    result.getPdfs(),
                    cloudBlockBlob
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
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(); // todo: inject
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(
                    zis, publicKeyDerFilename, zipFileName, containerName
                );

            return zipFileProcessor.process(zipWithSignature, ZipVerifiers.getPreprocessor(signatureAlg));
        } catch (DocSignatureFailureException ex) {
            log.warn("Rejected file {} from container {} - invalid signature", zipFileName, containerName, ex);
            handleEventRelatedError(Event.DOC_SIGNATURE_FAILURE, containerName, zipFileName, ex);
            blobManager.tryMoveFileToRejectedContainer(zipFileName, containerName, null);
            return null;
        } catch (Exception ex) {
            log.error("Failed to reprocess file {} from container {}", zipFileName, containerName, ex);
            handleEventRelatedError(Event.DOC_FAILURE, containerName, zipFileName, ex);
            return null;
        }
    }
}
