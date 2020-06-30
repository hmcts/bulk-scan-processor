package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.base.Strings;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

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
public class BlobProcessorTask {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final BlobManager blobManager;

    private final EnvelopeProcessor envelopeProcessor;

    private final FileContentProcessor fileContentProcessor;

    public BlobProcessorTask(
        BlobManager blobManager,
        EnvelopeProcessor envelopeProcessor,
        FileContentProcessor fileContentProcessor
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.fileContentProcessor = fileContentProcessor;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() {
        log.info("Started blob processing job");

        for (CloudBlobContainer container : blobManager.listInputContainers()) {
            processZipFiles(container);
        }

        log.info("Finished blob processing job");
    }

    private void processZipFiles(CloudBlobContainer container) {
        log.info("Processing blobs for container {}", container.getName());

        // Randomise iteration order to minimise lease acquire contention
        // For this purpose it's more efficient to have a collection that
        // implements RandomAccess (e.g. ArrayList)
        List<String> zipFilenames = new ArrayList<>();
        container
            .listBlobs()
            .forEach(b -> {
                String fileName = FilenameUtils.getName(b.getUri().toString());
                if (Strings.isNullOrEmpty(fileName)) {
                    log.error("Cannot extract filename from list blob item. URI: {}", b.getUri());
                } else {
                    zipFilenames.add(fileName);
                }
            });
        Collections.shuffle(zipFilenames);
        for (String zipFilename : zipFilenames) {
            tryProcessZipFile(container, zipFilename);
        }

        log.info("Finished processing blobs for container {}", container.getName());
    }

    private void tryProcessZipFile(CloudBlobContainer container, String zipFilename) {
        try {
            processZipFileIfEligible(container, zipFilename);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, container.getName(), ex);
        }
    }

    private void processZipFileIfEligible(CloudBlobContainer container, String zipFilename)
        throws IOException, StorageException, URISyntaxException {
        // this log entry is used in alerting. Ticket: BPS-541
        log.info("Processing zip file {} from container {}", zipFilename, container.getName());

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);

        Optional<UUID> existingEnvelopeIdOpt =
            envelopeProcessor.getEnvelopeIdByFileAndContainer(container.getName(), zipFilename);

        if (existingEnvelopeIdOpt.isPresent()) {
            abortProcessingEnvelope(container, zipFilename, existingEnvelopeIdOpt.get());
        } else {
            tryLeaseAndProcessZipFile(container, zipFilename, cloudBlockBlob);
        }
    }

    private void abortProcessingEnvelope(CloudBlobContainer container, String zipFilename, UUID existingEnvelopeId) {
        log.warn(
            "Envelope for zip file {} (container {}) already exists. Aborting its processing. Envelope ID: {}",
            zipFilename,
            container.getName(),
            existingEnvelopeId
        );
    }

    private void tryLeaseAndProcessZipFile(
        CloudBlobContainer container,
        String zipFilename,
        CloudBlockBlob cloudBlockBlob
    ) throws StorageException, IOException {
        if (!cloudBlockBlob.exists()) {
            logAbortedProcessingNonExistingFile(zipFilename, container.getName());
        } else {
            cloudBlockBlob.downloadAttributes();
            leaseAndProcessZipFile(container, cloudBlockBlob, zipFilename);
        }
    }

    private void leaseAndProcessZipFile(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        String zipFilename
    ) throws StorageException, IOException {
        Optional<String> leaseIdOption = blobManager.acquireLease(cloudBlockBlob, container.getName(), zipFilename);

        if (leaseIdOption.isPresent()) {
            String leaseId = leaseIdOption.get();

            try {
                processZipFile(container, cloudBlockBlob, zipFilename, leaseId);
            } finally {
                blobManager.tryReleaseLease(cloudBlockBlob, container.getName(), zipFilename, leaseId);
            }
        }
    }

    private void processZipFile(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        String zipFilename,
        String leaseId
    ) throws StorageException, IOException {
        Optional<UUID> existingEnvelopeIdOpt =
            envelopeProcessor.getEnvelopeIdByFileAndContainer(container.getName(), zipFilename);

        if (existingEnvelopeIdOpt.isPresent()) {
            abortProcessingEnvelope(container, zipFilename, existingEnvelopeIdOpt.get());
        } else {
            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = loadIntoMemory(cloudBlockBlob, zipFilename)) {
                envelopeProcessor.createEvent(
                    ZIPFILE_PROCESSING_STARTED,
                    container.getName(),
                    zipFilename,
                    null,
                    null
                );

                fileContentProcessor.processZipFileContent(zis, zipFilename, container.getName(), leaseId);
            }
        }
    }

    private ZipInputStream loadIntoMemory(CloudBlockBlob cloudBlockBlob, String zipFilename) throws StorageException {
        log.info("Loading file {} into memory.", zipFilename);
        try (BlobInputStream blobInputStream = cloudBlockBlob.openInputStream()) {
            byte[] array = toByteArray(blobInputStream);
            log.info(
                "Finished loading file {} into memory. {} loaded.",
                zipFilename,
                FileUtils.byteCountToDisplaySize(array.length)
            );
            return new ZipInputStream(new ByteArrayInputStream(array));
        } catch (IOException exception) {
            throw new ZipFileLoadException("Error loading blob file " + zipFilename, exception);
        }
    }

    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }
}
