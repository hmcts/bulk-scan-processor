package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

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
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocUploadFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeAwareThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NoPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public void processBlobs() throws Exception {
        for (CloudBlobContainer container : cloudBlobClient.listContainers()) {
            processZipFiles(container);
        }
    }

    private void processZipFiles(CloudBlobContainer container) throws Exception {
        log.info("Processing blobs for container {}", container.getName());

        for (ListBlobItem blobItem : container.listBlobs()) {
            String zipFilename = FilenameUtils.getName(blobItem.getUri().toString());

            processZipFile(container, zipFilename);
        }
    }

    private void processZipFile(CloudBlobContainer container, String zipFilename) throws Exception {
        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);
        BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();

        //Zip file will include metadata.json and collection of pdf documents
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            Map<Envelope, List<Pdf>> envelopeMap = processZipFileEntry(zis, zipFilename, container.getName());

            for (Map.Entry<Envelope, List<Pdf>> entry : envelopeMap.entrySet()) {
                processParsedEnvelopeDocuments(entry.getKey(), entry.getValue(), cloudBlockBlob);
            }
        }
    }

    private Map<Envelope, List<Pdf>> processZipFileEntry(
        ZipInputStream zis,
        String zipFilename,
        String containerName
    ) throws Exception {
        try {
            List<Pdf> pdfFiles = new ArrayList<>();
            byte[] metadataStream = null;
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                switch (FilenameUtils.getExtension(zipEntry.getName())) {
                    case "json":
                        metadataStream = toByteArray(zis);

                        break;
                    case "pdf":
                        pdfFiles.add(new Pdf(zipEntry.getName(), toByteArray(zis)));

                        break;
                    default:
                        // contract breakage
                        throw new NoPdfFileFoundException(containerName, zipFilename);
                }
            }

            log.info("PDFs found in {}: {}", zipFilename, pdfFiles.size());

            Envelope envelope = envelopeProcessor.processEnvelope(metadataStream, containerName);

            return Collections.singletonMap(envelope, pdfFiles);
        } catch (Exception exception) {
            throw Optional.of(exception)
                .filter(EventRelatedThrowable.class::isInstance)
                .orElse(new DocFailureGenericException(containerName, zipFilename, exception));
        }
    }

    private void processParsedEnvelopeDocuments(
        Envelope envelope,
        List<Pdf> pdfs,
        CloudBlockBlob cloudBlockBlob
    ) throws Exception {
        try {
            documentProcessor.processPdfFiles(pdfs, envelope.getScannableItems());
            envelopeProcessor.markAsUploaded(envelope);

            cloudBlockBlob.delete();

            envelopeProcessor.markAsProcessed(envelope);
        } catch (Exception exception) {
            throw Optional.of(exception)
                .filter(EnvelopeAwareThrowable.class::isInstance)
                .orElse(new DocUploadFailureGenericException(envelope, exception));
        }
    }
}
