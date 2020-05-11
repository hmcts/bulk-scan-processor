package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.base.Strings;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.errornotifications.ErrorMapping;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
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
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
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
public class BlobProcessorTask extends Processor {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final ZipFileProcessor zipFileProcessor;

    private final ServiceBusHelper notificationsQueueHelper;

    protected final ContainerMappings containerMappings;

    private final OcrValidator ocrValidator;

    private final boolean paymentsEnabled;

    @SuppressWarnings("squid:S00107")
    @Autowired
    public BlobProcessorTask(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ZipFileProcessor zipFileProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        ContainerMappings containerMappings,
        OcrValidator ocrValidator,
        @Qualifier("notifications-helper") ServiceBusHelper notificationsQueueHelper,
        @Value("${process-payments.enabled}") boolean paymentsEnabled
    ) {
        super(blobManager, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
        this.zipFileProcessor = zipFileProcessor;
        this.notificationsQueueHelper = notificationsQueueHelper;
        this.containerMappings = containerMappings;
        this.ocrValidator = ocrValidator;
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

                ZipFileProcessingResult processingResult =
                    processZipFileContent(zis, zipFilename, container.getName(), leaseId);

                if (processingResult != null) {
                    processParsedEnvelopeDocuments(
                        processingResult.getEnvelope(),
                        processingResult.getPdfs()
                    );
                }
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
        try (BlobInputStream blobInputStream = cloudBlockBlob.openInputStream()) {
            byte[] array = toByteArray(blobInputStream);
            return new ZipInputStream(new ByteArrayInputStream(array));
        } catch (IOException exception) {
            throw new ZipFileLoadException("Error loading blob file " + zipFilename, exception);
        }
    }

    private ZipFileProcessingResult processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName,
        String leaseId
    ) {
        try {
            ZipFileProcessingResult result = zipFileProcessor.process(zis, zipFilename);

            InputEnvelope envelope = envelopeProcessor.parseEnvelope(result.getMetadata(), zipFilename);

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

            Optional<OcrValidationWarnings> ocrValidationWarnings;

            if (envelope.classification != EXCEPTION) {
                ocrValidationWarnings = this.ocrValidator.assertOcrDataIsValid(envelope);
            } else {
                ocrValidationWarnings = Optional.empty();
            }

            Envelope dbEnvelope = toDbEnvelope(envelope, containerName, ocrValidationWarnings);

            result.setEnvelope(envelopeProcessor.saveEnvelope(dbEnvelope));

            return result;
        } catch (PaymentsDisabledException ex) {
            log.error(
                "Rejected file {} from container {} - Payments processing is disabled", zipFilename, containerName
            );
            handleInvalidFileError(Event.FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
            return null;
        } catch (ServiceDisabledException ex) {
            log.error(
                "Rejected file {} from container {} - Service is disabled", zipFilename, containerName
            );
            handleInvalidFileError(Event.DISABLED_SERVICE_FAILURE, containerName, zipFilename, leaseId, ex);
            return null;
        } catch (InvalidEnvelopeException ex) {
            log.warn("Rejected file {} from container {} - invalid", zipFilename, containerName, ex);
            handleInvalidFileError(Event.FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
            return null;
        } catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {} - failed previously", zipFilename, containerName, ex);
            createEvent(Event.DOC_UPLOAD_FAILURE, containerName, zipFilename, ex);
            return null;
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            createEvent(Event.DOC_FAILURE, containerName, zipFilename, ex);
            return null;
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
        Exception cause
    ) {
        Long eventId = envelopeProcessor.createEvent(
            fileValidationFailure,
            containerName,
            zipFilename,
            cause.getMessage(),
            null
        );

        ErrorMapping
            .getFor(cause.getClass())
            .ifPresent(errorCode -> {
                try {
                    sendErrorMessageToQueue(zipFilename, containerName, eventId, errorCode, cause);
                } catch (Exception exc) {
                    log.error(
                        "Error sending notification to the queue."
                            + "File name: " + zipFilename + " "
                            + "Container: " + containerName,
                        exc
                    );
                }
            });

        blobManager.tryMoveFileToRejectedContainer(zipFilename, containerName, leaseId);
    }

    private void sendErrorMessageToQueue(
        String zipFilename,
        String containerName,
        Long eventId,
        ErrorCode errorCode,
        Exception cause
    ) {
        String messageId = UUID.randomUUID().toString();

        this.notificationsQueueHelper.sendMessage(
            new ErrorMsg(
                messageId,
                eventId,
                zipFilename,
                containerName,
                getPoBox(containerName),
                null,
                errorCode,
                cause.getMessage(),
                "bulk_scan_processor",
                containerName
            )
        );

        log.info(
            "Created error notification for file {} in container {}. Queue message ID: {}",
            zipFilename,
            containerName,
            messageId
        );
    }

    private String getPoBox(String containerName) {
        return containerMappings
            .getMappings()
            .stream()
            .filter(m -> m.getContainer().equals(containerName))
            .map(ContainerMappings.Mapping::getPoBox)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Mapping not found for container " + containerName));
    }

    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }
}
