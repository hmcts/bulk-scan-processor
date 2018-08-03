package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things
 * 1. Reads Blob from container.
 * 2. Extract Zip file(Blob)
 * 3. Transform metadata json to DB entities.
 * 4. Save PDF files in document storage.
 * 5. Update status and doc urls in DB.
 */
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class BlobProcessorTask {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    private final EnvelopeProcessor envelopeProcessor;
    private final ErrorHandlingWrapper errorWrapper;

    public BlobProcessorTask(
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

    @SchedulerLock(name = "blobProcessor")
    @Scheduled(fixedDelayString = "${scan.delay}")
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
        BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();

        //Zip file will include metadata.json and collection of pdf documents
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            ZipFileProcessor zipFileProcessor = processZipInputStream(zis, zipFilename, container.getName());

            if (zipFileProcessor != null) {
                processParsedEnvelopeDocuments(
                    zipFileProcessor.getEnvelope(),
                    zipFileProcessor.getPdfs(),
                    cloudBlockBlob
                );
            }
        }
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

            Optional<Envelope> optionalEnvelope = envelopeProcessor.hasEnvelopeFailedToUploadBefore(envelope);

            if (optionalEnvelope.isPresent()) {
                throw new PreviouslyFailedToUploadException(
                    containerName,
                    zipFilename,
                    String.format(
                        "Envelope %s created at %s is already marked as failed to upload. Skipping",
                        optionalEnvelope.get().getId(),
                        optionalEnvelope.get().getCreatedAt()
                    )
                );
            } else {
                zipFileProcessor.setEnvelope(envelopeProcessor.saveEnvelope(envelope));
                return zipFileProcessor;
            }
        });
    }

    private void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        CloudBlockBlob cloudBlockBlob
    ) {
        errorWrapper.wrapDocUploadFailure(envelope, () -> {
            documentProcessor.processPdfFiles(pdfs, envelope.getScannableItems());
            envelopeProcessor.markAsUploaded(envelope);

            cloudBlockBlob.delete();

            envelopeProcessor.markAsProcessed(envelope);

            return null;
        });
    }
}
