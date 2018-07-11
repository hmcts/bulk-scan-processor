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
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.io.ByteStreams.toByteArray;

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

    public BlobProcessorTask(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
    }

    @SchedulerLock(name = "blobProcessor")
    @Scheduled(fixedDelayString = "${scan.delay}")
    public void processBlobs() {
        cloudBlobClient.listContainers()
            .forEach(this::processZipFiles);
    }

    private void processZipFiles(CloudBlobContainer container) {
        log.info("Processing blobs for container {} ", container.getName());

        for (ListBlobItem blobItem : container.listBlobs()) {
            String zipFilename = FilenameUtils.getName(blobItem.getUri().toString());

            try {
                processZipFile(container, zipFilename);
            } catch (Exception e) {
                //If any error occurs processing one record remaining records should be continued processing
                log.error("Exception occurred while processing zip file " + zipFilename, e);
                // TODO A record in Database needs to created with appropriate status.
            }
        }
    }

    private void processZipFile(CloudBlobContainer container, String zipFilename)
        throws StorageException, URISyntaxException, IOException {

        List<Pdf> pdfFiles = new ArrayList<>();
        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);
        BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();
        boolean isUploadFailure = false;
        Envelope envelope = null;

        //Zip file will include metadata.json and collection of pdf documents
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            byte[] metadataStream = null;
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                switch (FilenameUtils.getExtension(zipEntry.getName())) {
                    case "json":
                        metadataStream = toByteArray(zis);
                        break;
                    case "pdf":
                        Pdf pdf = new Pdf(zipEntry.getName(), toByteArray(zis));
                        pdfFiles.add(pdf);
                        break;
                    default:
                        //Contract breakage
                        throw new NoPdfFileFoundException(
                            "Zip file contains non pdf documents for file " + zipFilename
                        );
                }
            }

            envelope = envelopeProcessor.processEnvelope(metadataStream);
            isUploadFailure = true;

            documentProcessor.processPdfFiles(pdfFiles, envelope.getScannableItems());
            envelopeProcessor.markAsUploaded(envelope, container.getName(), zipFilename);

            cloudBlockBlob.delete();
        } catch (Exception exception) {
            markAsFailed(isUploadFailure, container.getName(), zipFilename, exception.getMessage(), envelope);

            throw exception;
        }
    }

    private void markAsFailed(
        boolean isUploadFailure,
        String container,
        String zipFileName,
        String message,
        Envelope envelope
    ) {
        EnvelopeProcessor.FailureMarker marker = isUploadFailure
            ? envelopeProcessor::markAsUploadFailed
            : envelopeProcessor::markAsGenericFailure;

        marker.markAsFailure(message, envelope, container, zipFileName);
    }
}
