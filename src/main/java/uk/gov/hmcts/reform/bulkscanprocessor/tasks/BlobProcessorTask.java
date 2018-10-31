package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlobAccessConditions;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.models.BlobGetPropertiesResponse;
import com.microsoft.azure.storage.blob.models.ContainerItem;
import com.microsoft.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.microsoft.azure.storage.blob.models.LeaseStateType;
import com.microsoft.azure.storage.blob.models.ModifiedAccessConditions;
import com.microsoft.azure.storage.blob.models.StorageErrorException;
import com.microsoft.rest.v2.Context;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.bulkscanprocessor.util.AzureStorageHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things:
 * <ol>
 * <li>Read Blob from container by acquiring lease</li>
 * <li>Extract Zip file (blob)</li>
 * <li>Transform metadata json to DB entities</li>
 * <li>Save PDF files in document storage</li>
 * <li>Update status and doc urls in DB</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class BlobProcessorTask extends Processor {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    // TODO check if this is used somewhere or if I removed it accidentally
    private final Integer blobLeaseTimeout;
    private final int blobProcessingDelayInMinutes;

    @Autowired
    public BlobProcessorTask(
        AzureStorageHelper azureStorageHelper,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper,
        @Value("${storage.signature_algorithm}") String signatureAlg,
        @Value("${storage.public_key_der_file}") String publicKeyDerFilename,
        @Value("${storage.blob_lease_timeout}") Integer blobLeaseTimeout,
        @Value("${storage.blob_processing_delay_in_minutes}") int blobProcessingDelayInMinutes
    ) {
        super(azureStorageHelper, documentProcessor, envelopeProcessor, errorWrapper, signatureAlg, publicKeyDerFilename);
        this.blobLeaseTimeout = blobLeaseTimeout;
        this.blobProcessingDelayInMinutes = blobProcessingDelayInMinutes;
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

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() throws IOException {
        List<ContainerItem> containerItems = azureStorageHelper.listContainers().blockingGet().body().containerItems()
            .stream()
            .filter(c -> !c.name().equals("$web"))
            .collect(toList());

        log.info("Processing blobs in containers: {}", containerItems.stream()
            .map(ContainerItem::name)
            .collect(toList()));

        for (ContainerItem container : containerItems) {
            processZipFiles(container);
        }
    }

    private void processZipFiles(ContainerItem container)
        throws IOException {
        log.info("Processing blobs for container {}", container.name());

        ServiceBusHelper serviceBusHelper = serviceBusHelper();

        // Randomise iteration order to minimise lease acquire contention
        // For this purpose it's more efficient to have a collection that
        // implements RandomAccess (e.g. ArrayList)
        List<String> zipFilenames = new ArrayList<>();
        ContainerURL containerURL = azureStorageHelper.getClient().createContainerURL(container.name());

        try {
            azureStorageHelper.listBlobsLazy(containerURL)
                .blockingIterable()
                .forEach(
                    b -> zipFilenames.add(FilenameUtils.getName(b.name()))
                );
            Collections.shuffle(zipFilenames);
            for (String zipFilename : zipFilenames) {
                processZipFile(container.name(), containerURL, zipFilename, serviceBusHelper);
            }
        } finally {
//            if (serviceBusHelper != null) {
//                serviceBusHelper.close();
//            }
        }
    }

    private void processZipFile(String containerName, ContainerURL containerURL, String zipFilename, ServiceBusHelper serviceBusHelper)
        throws IOException {


        BlockBlobURL blockBlobURL = containerURL.createBlockBlobURL(zipFilename);

        BlobGetPropertiesResponse properties = blockBlobURL
            .getProperties(new BlobAccessConditions(), Context.NONE)
            .blockingGet();

        if (!isReadyToBeProcessed(properties.headers().lastModified())) {
            return;
        }

        log.info("Processing zip file {}", zipFilename);

        Envelope existingEnvelope =
            envelopeProcessor.getEnvelopeByFileAndContainer(containerName, zipFilename);
        if (existingEnvelope != null) {
            deleteIfProcessed(blockBlobURL, existingEnvelope);
            return;
        }

        LeaseStateType leaseStateType = properties.headers().leaseState();
        boolean leaseAvailable = azureStorageHelper.checkLeaseAvailable(containerName, zipFilename, leaseStateType);

        if (leaseAvailable) {
            byte[] blob = azureStorageHelper.downloadBlob(blockBlobURL).blockingGet().array();

            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(blob))) {
                ZipFileProcessor zipFileProcessor = processZipFileContent(zis, zipFilename, containerName);

                if (zipFileProcessor != null) {
                    processParsedEnvelopeDocuments(
                        zipFileProcessor.getEnvelope(),
                        zipFileProcessor.getPdfs(),
                        blockBlobURL,
                        serviceBusHelper
                    );
                }
            }
        }
    }

    private void deleteIfProcessed(BlockBlobURL blockBlobURL, Envelope envelope) {
        try {
            if (envelope.getStatus().isProcessed()) {
                blockBlobURL.delete(DeleteSnapshotsOptionType.INCLUDE,
                    new BlobAccessConditions().withModifiedAccessConditions(
                        // Wildcard will match any etag.
                        new ModifiedAccessConditions().withIfMatch("*")), null)
                    .blockingGet();

                envelope.setZipDeleted(true);
                envelopeProcessor.saveEnvelope(envelope);
            }
        } catch (StorageErrorException e) {
            log.warn("Failed to delete blob [{}]", blockBlobURL.toURL());
        } // Do not propagate exception as this blob has already been marked with a delete failure
    }

    private ZipFileProcessor processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) {
        return errorWrapper.wrapDocFailure(containerName, zipFilename, () -> {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(containerName, zipFilename);
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(zis, publicKeyDerFilename, zipFilename, containerName);
            zipFileProcessor.process(zipWithSignature, ZipVerifiers.getPreprocessor(signatureAlg));

            Envelope envelope = envelopeProcessor.parseEnvelope(zipFileProcessor.getMetadata(), zipFilename);
            envelope.setContainer(containerName);

            EnvelopeProcessor.assertEnvelopeHasPdfs(envelope, zipFileProcessor.getPdfs());
            envelopeProcessor.assertDidNotFailToUploadBefore(envelope);

            zipFileProcessor.setEnvelope(envelopeProcessor.saveEnvelope(envelope));

            return zipFileProcessor;
        });
    }

    private boolean isReadyToBeProcessed(OffsetDateTime lastModifiedDate) {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(this.blobProcessingDelayInMinutes, ChronoUnit.MINUTES);

        return lastModifiedDate.isBefore(cutoff);
    }
}
