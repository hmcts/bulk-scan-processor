package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

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
@ConditionalOnExpression("!${jms.enabled}")
public class BlobProcessorTask {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final BlobManager blobManager;

    private final EnvelopeProcessor envelopeProcessor;

    private final FileContentProcessor fileContentProcessor;

    private final  LeaseAcquirer leaseAcquirer;

    private final OcrValidationRetryManager ocrValidationRetryManager;

    /**
     * Constructor for the BlobProcessorTask.
     * @param blobManager The blob manager
     * @param envelopeProcessor The envelope processor
     * @param fileContentProcessor The file content processor
     * @param leaseAcquirer The lease acquirer
     * @param ocrValidationRetryManager The OCR validation retry manager
     */
    public BlobProcessorTask(
        BlobManager blobManager,
        EnvelopeProcessor envelopeProcessor,
        FileContentProcessor fileContentProcessor,
        LeaseAcquirer leaseAcquirer,
        OcrValidationRetryManager ocrValidationRetryManager
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.fileContentProcessor = fileContentProcessor;
        this.leaseAcquirer = leaseAcquirer;
        this.ocrValidationRetryManager = ocrValidationRetryManager;
    }

    /**
     * Process blobs from input containers.
     */
    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() {
        log.info("Started blob processing job");

        for (BlobContainerClient container : blobManager.listInputContainerClients()) {
            processZipFiles(container);
        }

        log.info("Finished blob processing job");
    }

    /**
     * Process zip files in the given container.
     * @param container The container
     */
    private void processZipFiles(BlobContainerClient container) {
        log.debug("Processing blobs for container {}", container.getBlobContainerName());
        List<String> zipFilenames = getShuffledZipFileNames(container);

        for (String zipFilename : zipFilenames) {
            tryProcessZipFile(container, zipFilename);
        }

        log.debug("Finished processing blobs for container {}", container.getBlobContainerName());
    }

    /**
     * Process a zip file.
     * @param container The container
     * @param zipFilename The zip file name
     */
    private void tryProcessZipFile(BlobContainerClient container, String zipFilename) {
        try {
            processZipFileIfEligible(container, zipFilename);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, container.getBlobContainerName(), ex);
        }
    }

    /**
     * Process a zip file if it is eligible.
     * @param container The container
     * @param zipFilename The zip file name
     */
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

    /**
     * Lease and process a zip file.
     * @param container The container
     * @param blobClient The blob client
     * @param zipFilename The zip file name
     */
    private void leaseAndProcessZipFile(
        BlobContainerClient container,
        BlobClient blobClient,
        String zipFilename
    ) {

        leaseAcquirer.ifAcquiredOrElse(
            blobClient,
            leaseId -> {
                if (ocrValidationRetryManager.canProcess(blobClient)) {
                    processZipFile(container, blobClient, zipFilename);
                }
            },
            s -> {},
            true
        );
    }

    /**
     * Process a zip file.
     * @param container The container
     * @param blobClient The blob client
     * @param zipFilename The zip file name
     */
    private void processZipFile(
        BlobContainerClient container,
        BlobClient blobClient,
        String zipFilename
    ) {
        Envelope envelope = envelopeProcessor
            .getEnvelopeByFileAndContainer(container.getBlobContainerName(), zipFilename);

        if (envelope == null) {
            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis =  new ZipInputStream(blobClient.openInputStream())) {
                envelopeProcessor.createEvent(
                    ZIPFILE_PROCESSING_STARTED,
                    container.getBlobContainerName(),
                    zipFilename,
                    null,
                    null
                );

                fileContentProcessor.processZipFileContent(
                    zis,
                    zipFilename,
                    container.getBlobContainerName()
                );

                log.info(
                    "Zip content processed for file {}, container: {}",
                    zipFilename,
                    container.getBlobContainerName()
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

    /**
     * Log that processing of a zip file was aborted because it doesn't exist anymore.
     * @param zipFilename The zip file name
     * @param containerName The container name
     */
    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }
}
