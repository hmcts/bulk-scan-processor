package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.zip.ZipInputStream;

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

    @Value("${storage.blob_lease_timeout}")
    private Integer blobLeaseTimeout;

    public BlobProcessorTask(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper
    ) {
        super(cloudBlobClient, documentProcessor, envelopeProcessor, errorWrapper);
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() throws IOException, StorageException, URISyntaxException {
        for (CloudBlobContainer container : cloudBlobClient.listContainers()) {
            processZipFiles(container);
        }
    }

    private void processZipFiles(CloudBlobContainer container)
        throws IOException, StorageException, URISyntaxException {
        log.info("Processing blobs for container {}", container.getName());

        for (ListBlobItem blobItem : container.listBlobs()) {
            String zipFilename = FilenameUtils.getName(blobItem.getUri().toString());

            processZipFile(container, zipFilename);
        }
    }

    private void processZipFile(CloudBlobContainer container, String zipFilename)
        throws IOException, StorageException, URISyntaxException {

        log.info("Processing zip file {}", zipFilename);

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);

        CloudBlockBlob blobWithLeaseAcquired = acquireLease(cloudBlockBlob, container.getName(), zipFilename);

        if (Objects.nonNull(blobWithLeaseAcquired)) {
            BlobInputStream blobInputStream = blobWithLeaseAcquired.openInputStream();

            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
                ZipFileProcessor zipFileProcessor = processZipInputStream(zis, zipFilename, container.getName());

                if (zipFileProcessor != null) {
                    processParsedEnvelopeDocuments(
                        zipFileProcessor.getEnvelope(),
                        zipFileProcessor.getPdfs(),
                        blobWithLeaseAcquired
                    );
                }
            }
        }
    }

    private CloudBlockBlob acquireLease(CloudBlockBlob cloudBlockBlob, String containerName, String zipFilename) {
        return errorWrapper.wrapAcquireLeaseFailure(containerName, zipFilename, () -> {
            cloudBlockBlob.acquireLease(blobLeaseTimeout, null);
            return cloudBlockBlob;
        });
    }

    private ZipFileProcessor processZipInputStream(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) {
        return errorWrapper.wrapDocFailure(containerName, zipFilename, () -> {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(containerName, zipFilename);
            zipFileProcessor.process(zis);

            Envelope envelope = envelopeProcessor.parseEnvelope(zipFileProcessor.getMetadata());
            envelope.setContainer(containerName);

            envelopeProcessor.assertDidNotFailToUploadBefore(envelope);

            zipFileProcessor.setEnvelope(envelopeProcessor.saveEnvelope(envelope));

            return zipFileProcessor;
        });
    }
}
