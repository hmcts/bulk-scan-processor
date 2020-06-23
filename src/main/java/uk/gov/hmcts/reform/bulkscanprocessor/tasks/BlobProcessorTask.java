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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeRejectionException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledExceptionEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledExceptionEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationSender;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper.toDbEnvelope;

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
@EnableConfigurationProperties(ContainerMappings.class)
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class BlobProcessorTask {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final BlobManager blobManager;

    private final EnvelopeProcessor envelopeProcessor;

    private final ZipFileProcessor zipFileProcessor;

    private final ContainerMappings containerMappings;

    private final OcrValidator ocrValidator;

    private final ErrorNotificationSender errorNotificationSender;

    private final boolean paymentsEnabled;

    public BlobProcessorTask(
        BlobManager blobManager,
        EnvelopeProcessor envelopeProcessor,
        ZipFileProcessor zipFileProcessor,
        ContainerMappings containerMappings,
        OcrValidator ocrValidator,
        ErrorNotificationSender errorNotificationSender,
        @Value("${process-payments.enabled}") boolean paymentsEnabled
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.zipFileProcessor = zipFileProcessor;
        this.containerMappings = containerMappings;
        this.ocrValidator = ocrValidator;
        this.errorNotificationSender = errorNotificationSender;
        this.paymentsEnabled = paymentsEnabled;
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

        Envelope existingEnvelope =
            envelopeProcessor.getEnvelopeByFileAndContainer(container.getName(), zipFilename);

        if (existingEnvelope != null) {
            log.warn(
                "Envelope for zip file {} (container {}) already exists. Aborting its processing. Envelope ID: {}",
                zipFilename,
                container.getName(),
                existingEnvelope.getId()
            );
        } else if (!cloudBlockBlob.exists()) {
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
        Envelope envelope = envelopeProcessor.getEnvelopeByFileAndContainer(container.getName(), zipFilename);

        if (envelope == null) {
            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = loadIntoMemory(cloudBlockBlob, zipFilename)) {
                createEvent(ZIPFILE_PROCESSING_STARTED, container.getName(), zipFilename, null);

                processZipFileContent(zis, zipFilename, container.getName(), leaseId);
            }
        } else {
            log.info(
                "Envelope already exists for container {} and file {} - aborting its processing. Envelope ID: {}",
                container.getName(),
                zipFilename,
                envelope.getId()
            );
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

    private void processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName,
        String leaseId
    ) {
        try {
            ZipFileProcessingResult result = zipFileProcessor.process(zis, zipFilename);

            InputEnvelope envelope = envelopeProcessor.parseEnvelope(result.getMetadata(), zipFilename);

            log.info(
                "Parsed envelope. File name: {}. Container: {}. Payment DCNs: {}. Document DCNs: {}",
                zipFilename,
                containerName,
                envelope.payments.stream().map(payment -> payment.documentControlNumber).collect(joining(",")),
                envelope.scannableItems.stream().map(doc -> doc.documentControlNumber).collect(joining(","))
            );

            EnvelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, zipFilename);
            EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                containerMappings.getMappings(), envelope, containerName
            );
            EnvelopeValidator.assertServiceEnabled(envelope, containerMappings.getMappings());
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope);
            EnvelopeValidator.assertEnvelopeHasPdfs(envelope, result.getPdfs());
            EnvelopeValidator.assertDocumentControlNumbersAreUnique(envelope);
            EnvelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                envelope, paymentsEnabled, containerMappings.getMappings()
            );
            EnvelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope);

            envelopeProcessor.assertDidNotFailToUploadBefore(envelope.zipFileName, containerName);

            Optional<OcrValidationWarnings> ocrValidationWarnings = this.ocrValidator.assertOcrDataIsValid(envelope);

            Envelope dbEnvelope = toDbEnvelope(envelope, containerName, ocrValidationWarnings);

            envelopeProcessor.saveEnvelope(dbEnvelope);
        } catch (PaymentsDisabledExceptionEnvelope ex) {
            log.error(
                "Rejected file {} from container {} - Payments processing is disabled", zipFilename, containerName
            );
            handleInvalidFileError(Event.FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (ServiceDisabledExceptionEnvelope ex) {
            log.error(
                "Rejected file {} from container {} - Service is disabled", zipFilename, containerName
            );
            handleInvalidFileError(Event.DISABLED_SERVICE_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (EnvelopeRejectionException ex) {
            log.warn("Rejected file {} from container {} - invalid", zipFilename, containerName, ex);
            handleInvalidFileError(Event.FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
        } catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {} - failed previously", zipFilename, containerName, ex);
            createEvent(Event.DOC_UPLOAD_FAILURE, containerName, zipFilename, ex);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            createEvent(Event.DOC_FAILURE, containerName, zipFilename, ex);
        }
    }

    private void createEvent(
        Event event,
        String containerName,
        String zipFilename,
        Exception exception
    ) {
        envelopeProcessor.createEvent(
            event,
            containerName,
            zipFilename,
            exception == null ? null : exception.getMessage(),
            null
        );
    }

    private void handleInvalidFileError(
        Event fileValidationFailure,
        String containerName,
        String zipFilename,
        String leaseId,
        EnvelopeRejectionException cause
    ) {
        Long eventId = envelopeProcessor.createEvent(
            fileValidationFailure,
            containerName,
            zipFilename,
            cause.getMessage(),
            null
        );

        errorNotificationSender.sendErrorNotification(
            zipFilename,
            containerName,
            cause,
            eventId,
            cause.getErrorCode()
        );
        blobManager.tryMoveFileToRejectedContainer(zipFilename, containerName, leaseId);

    }

    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }
}
