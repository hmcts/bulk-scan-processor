package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.blob.BlobAccessConditions;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.models.BlobGetPropertiesResponse;
import com.microsoft.rest.v2.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.Processor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.AzureStorageHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipInputStream;

@Component
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, // every other call to return bean will create new instance
    proxyMode = ScopedProxyMode.TARGET_CLASS // allows prototyping
)
public class FailedDocUploadProcessor extends Processor {

    private static final Logger log = LoggerFactory.getLogger(FailedDocUploadProcessor.class);

    private final ServiceBusHelper serviceBusHelper;

    @Autowired
    public FailedDocUploadProcessor(
        AzureStorageHelper azureStorageHelper,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper,
        @Value("${storage.signature_algorithm}") String signatureAlg,
        @Value("${storage.public_key_der_file}") String publicKeyDerFilename
    ) {
        super(azureStorageHelper, documentProcessor, envelopeProcessor, errorWrapper, signatureAlg, publicKeyDerFilename);
        this.serviceBusHelper = serviceBusHelper();
    }

    /**
     * Spring overrides the {@code @Lookup} method and returns an instance of bean.
     *
     * @return Instance of {@code ServiceBusHelper}
     */
    @Lookup
    public ServiceBusHelper serviceBusHelper() {
        return null;
    }

    public void processJurisdiction(String jurisdiction) throws IOException {

        List<Envelope> envelopes = envelopeProcessor.getFailedToUploadEnvelopes(jurisdiction);

        if (envelopes.size() > 0) {
            String containerName = envelopes.get(0).getContainer();

            processEnvelopes(containerName, envelopes);
        }
    }

    private void processEnvelopes(String containerName, List<Envelope> envelopes)
        throws IOException {

        log.info("Processing {} failed documents for container {}", envelopes.size(), containerName);

        try {
            ContainerURL container = azureStorageHelper.getClient()
                .createContainerURL(containerName);

            for (Envelope envelope : envelopes) {
                processEnvelope(containerName, container, envelope);
            }
        } finally {
            if (serviceBusHelper != null) {
                serviceBusHelper.close();
            }
        }
    }

    private void processEnvelope(String containerName, ContainerURL container, Envelope envelope)
        throws IOException {

        String zipFileName = envelope.getZipFileName();
        log.info("Processing zip file {}", zipFileName);

        BlockBlobURL blockBlobURL = container.createBlockBlobURL(zipFileName);

        BlobGetPropertiesResponse properties = blockBlobURL
            .getProperties(new BlobAccessConditions(), Context.NONE)
            .blockingGet();

        boolean leaseAvailable = azureStorageHelper
            .checkLeaseAvailable(containerName, zipFileName, properties.headers().leaseState());

        if (leaseAvailable) {
            byte[] blob = azureStorageHelper.downloadBlob(blockBlobURL).blockingGet().array();

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(blob))) {
                ZipFileProcessor zipFileProcessor = processZipInputStream(zis, envelope);

                if (zipFileProcessor != null) {
                    processParsedEnvelopeDocuments(
                        envelope,
                        zipFileProcessor.getPdfs(),
                        blockBlobURL,
                        serviceBusHelper
                    );
                }
            }
        }
    }

    private ZipFileProcessor processZipInputStream(ZipInputStream zis, Envelope envelope) {
        return errorWrapper.wrapDocFailure(envelope.getContainer(), envelope.getZipFileName(), () -> {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(envelope);
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(
                    zis, publicKeyDerFilename, envelope.getZipFileName(), envelope.getContainer()
                );
            zipFileProcessor.process(zipWithSignature, ZipVerifiers.getPreprocessor(signatureAlg));

            return zipFileProcessor;
        });
    }
}
