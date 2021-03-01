package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.util.zip.ZipInputStream;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@Service
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class ZipFileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ZipFileProcessingService.class);

    private final EnvelopeProcessor envelopeProcessor;

    private final FileContentProcessor fileContentProcessor;

    private final LeaseAcquirer leaseAcquirer;

    private final OcrValidationRetryManager ocrValidationRetryManager;

    public ZipFileProcessingService(
        EnvelopeProcessor envelopeProcessor,
        FileContentProcessor fileContentProcessor,
        LeaseAcquirer leaseAcquirer,
        OcrValidationRetryManager ocrValidationRetryManager
    ) {
        this.envelopeProcessor = envelopeProcessor;
        this.fileContentProcessor = fileContentProcessor;
        this.leaseAcquirer = leaseAcquirer;
        this.ocrValidationRetryManager = ocrValidationRetryManager;
    }

    public void tryProcessZipFile(BlobContainerClient container, String zipFilename) {
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
            leaseId -> {
                if (ocrValidationRetryManager.canProcess(blobClient)) {
                    processZipFile(container, blobClient, zipFilename, leaseId);
                }
            },
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
                    container.getBlobContainerName(),
                    leaseId
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

    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }
}
