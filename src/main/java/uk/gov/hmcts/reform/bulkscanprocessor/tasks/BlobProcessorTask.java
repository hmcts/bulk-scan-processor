package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipInputStream;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.FileNamesExtractor.getShuffledZipFileNames;

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

    private final  LeaseAcquirer leaseAcquirer;

    public BlobProcessorTask(
        BlobManager blobManager,
        EnvelopeProcessor envelopeProcessor,
        FileContentProcessor fileContentProcessor,
        LeaseAcquirer leaseAcquirer
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.fileContentProcessor = fileContentProcessor;
        this.leaseAcquirer = leaseAcquirer;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() {
        log.info("Started blob processing job");

        for (BlobContainerClient container : blobManager.listInputContainerClients()) {
            processZipFiles(container);
        }

        log.info("Finished blob processing job");
    }

    private void processZipFiles(BlobContainerClient container) {
        log.info("Processing blobs for container {}", container.getBlobContainerName());
        List<String> zipFilenames = getShuffledZipFileNames(container);

        for (String zipFilename : zipFilenames) {
            tryProcessZipFile(container, zipFilename);
        }

        log.info("Finished processing blobs for container {}", container.getBlobContainerName());
    }

    private void tryProcessZipFile(BlobContainerClient container, String zipFilename) {
        try {
            processZipFileIfEligible(container, zipFilename);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, container.getBlobContainerName(), ex);
        }
    }

    private void processZipFileIfEligible(BlobContainerClient container, String zipFilename) {
        // this log entry is used in alerting. Ticket: BPS-541
        log.info("Processing zip file {} from container {}", zipFilename, container.getBlobContainerName());

        BlobClient blobClient = container.getBlobClient(zipFilename);

        Envelope existingEnvelope =
            envelopeProcessor.getEnvelopeByFileAndContainer(container.getBlobContainerName(), zipFilename);

        if (existingEnvelope != null) {
            log.warn(
                "Envelope for zip file {} (container {}) already exists. Aborting its processing. Envelope ID: {}",
                zipFilename,
                container.getBlobContainerName(),
                existingEnvelope.getId()
            );
        } else if (Boolean.FALSE.equals(blobClient.exists())) {
            logAbortedProcessingNonExistingFile(zipFilename, container.getBlobContainerName());
        } else {
            leaseAndProcessZipFile(container, blobClient, zipFilename);
        }
    }

    private void leaseAndProcessZipFile(
        BlobContainerClient container,
        BlobClient blobClient,
        String zipFilename
    ) {

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            leaseId -> processZipFile(container, blobClient, zipFilename, leaseId),
            s -> {},
            true
        );

    }

    private void processZipFile(
        BlobContainerClient container,
        BlobClient blobClient,
        String zipFilename,
        String leaseId
    ) {
        Envelope envelope = envelopeProcessor
            .getEnvelopeByFileAndContainer(container.getBlobContainerName(), zipFilename);

        if (envelope == null) {
            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = loadIntoMemory(blobClient, zipFilename)) {
                envelopeProcessor.createEvent(
                    ZIPFILE_PROCESSING_STARTED,
                    container.getBlobContainerName(),
                    zipFilename,
                    null,
                    null
                );

                fileContentProcessor.processZipFileContent(zis, zipFilename, container.getBlobContainerName(), leaseId);

                log.info(
                    "Zip content processed for file {}, container {}, envelope ID: {}",
                    container.getBlobContainerName(),
                    zipFilename,
                    envelope.getId()
                );

            } catch (IOException exception) {
                throw new ZipFileLoadException("Error loading blob file " + zipFilename, exception);
            }
        } else {
            log.info(
                "Envelope already exists for container {} and file {} - aborting its processing. Envelope ID: {}",
                container.getBlobContainerName(),
                zipFilename,
                envelope.getId()
            );
        }
    }

    private ZipInputStream loadIntoMemory(BlobClient blobClient, String zipFilename) {
        log.info("Loading file {} into memory.", zipFilename);
        try (var outputStream = new ByteArrayOutputStream()) {
            blobClient.download(outputStream);
            byte[] array = outputStream.toByteArray();
            log.info(
                "Finished loading file {} into memory. {} loaded.",
                zipFilename,
                FileUtils.byteCountToDisplaySize(array.length)
            );
            return new ZipInputStream(new ByteArrayInputStream(array));
        } catch (IOException exception) {
            throw new ZipFileLoadException("Error loading into memory, blob file " + zipFilename, exception);
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
