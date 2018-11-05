package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
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
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper
    ) {
        super(cloudBlobClient, documentProcessor, envelopeProcessor, errorWrapper);
    }

    // NOTE: this is needed for testing as children of this class are instantiated
    // using "new" in tests despite being spring beans (sigh!)
    public FailedDocUploadProcessor(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper,
        String signatureAlg,
        String publicKeyDerFilename
    ) {
        this(cloudBlobClient, documentProcessor, envelopeProcessor, errorWrapper);
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
    }

    public void processJurisdiction(String jurisdiction)
        throws IOException, StorageException, URISyntaxException {

        List<Envelope> envelopes = envelopeProcessor.getFailedToUploadEnvelopes(jurisdiction);

        if (!envelopes.isEmpty()) {
            String containerName = envelopes.get(0).getContainer();

            processEnvelopes(containerName, envelopes);
        }
    }

    private void processEnvelopes(String containerName, List<Envelope> envelopes)
        throws IOException, StorageException, URISyntaxException {

        log.info("Processing {} failed documents for container {}", envelopes.size(), containerName);

        CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);

        for (Envelope envelope : envelopes) {
            processEnvelope(container, envelope);
        }
    }

    private void processEnvelope(CloudBlobContainer container, Envelope envelope)
        throws IOException, StorageException, URISyntaxException {

        log.info("Processing zip file {}", envelope.getZipFileName());

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(envelope.getZipFileName());
        BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();

        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            ZipFileProcessor zipFileProcessor = processZipInputStream(
                zis,
                envelope.getContainer(),
                envelope.getZipFileName()
            );

            if (zipFileProcessor != null) {
                processParsedEnvelopeDocuments(
                    envelope,
                    zipFileProcessor.getPdfs(),
                    cloudBlockBlob
                );
            }
        }
    }

    private ZipFileProcessor processZipInputStream(
        ZipInputStream zis,
        String containerName,
        String zipFileName
    ) {
        return errorWrapper.wrapDocFailure(containerName, zipFileName, () -> {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(); // todo: inject
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(
                    zis, publicKeyDerFilename, zipFileName, containerName
                );

            zipFileProcessor.process(zipWithSignature, ZipVerifiers.getPreprocessor(signatureAlg));

            return zipFileProcessor;
        });
    }
}
