package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.base.Strings;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumbersInEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
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
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipInputStream;

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

    @Value("${storage.blob_processing_delay_in_minutes}")
    protected int blobProcessingDelayInMinutes;

    private final ServiceBusHelper notificationsQueueHelper;

    protected final ContainerMappings containerMappings;

    @Autowired
    public BlobProcessorTask(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        ContainerMappings containerMappings,
        @Qualifier("notifications-helper") ServiceBusHelper notificationsQueueHelper
    ) {
        super(blobManager, documentProcessor, envelopeProcessor, envelopeRepository, eventRepository);
        this.notificationsQueueHelper = notificationsQueueHelper;
        this.containerMappings = containerMappings;
    }

    // NOTE: this is needed for testing as children of this class are instantiated
    // using "new" in tests despite being spring beans (sigh!)
    @SuppressWarnings("squid:S00107")
    public BlobProcessorTask(
        BlobManager blobManager,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository eventRepository,
        ContainerMappings containerMappings,
        @Qualifier("notifications-helper") ServiceBusHelper notificationsQueueHelper,
        String signatureAlg,
        String publicKeyDerFilename
    ) {
        this(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            envelopeRepository,
            eventRepository,
            containerMappings,
            notificationsQueueHelper
        );
        this.signatureAlg = signatureAlg;
        this.publicKeyDerFilename = publicKeyDerFilename;
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
            deleteIfProcessed(cloudBlockBlob, existingEnvelope, container.getName());
        } else if (!cloudBlockBlob.exists()) {
            logAbortedProcessingNonExistingFile(zipFilename, container.getName());
        } else {
            cloudBlockBlob.downloadAttributes();

            if (!isReadyToBeProcessed(cloudBlockBlob)) {
                logAbortedProcessingNotReadyFile(zipFilename, container.getName());
            } else {
                processZipFile(container, cloudBlockBlob, zipFilename);
            }
        }
    }

    private void processZipFile(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        String zipFilename
    ) throws StorageException, IOException {
        Optional<String> leaseId = blobManager.acquireLease(cloudBlockBlob, container.getName(), zipFilename);

        if (leaseId.isPresent()) {
            BlobInputStream blobInputStream = cloudBlockBlob.openInputStream();

            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
                registerEvent(ZIPFILE_PROCESSING_STARTED, container.getName(), zipFilename, null);

                ZipFileProcessingResult processingResult =
                    processZipFileContent(zis, zipFilename, container.getName(), leaseId.get());

                if (processingResult != null) {
                    processParsedEnvelopeDocuments(
                        processingResult.getEnvelope(),
                        processingResult.getPdfs(),
                        cloudBlockBlob
                    );
                }
            }
        }
    }

    private void deleteIfProcessed(CloudBlockBlob cloudBlockBlob, Envelope envelope, String containerName) {
        String blobName = cloudBlockBlob.getName();
        log.info("Considering the deletion of file {} in container {}", blobName, containerName);

        try {
            if (isReadyToBeDeleted(envelope)) {
                log.info("File {} (container {}) is processed - deleting", blobName, containerName);

                boolean deleted;
                if (cloudBlockBlob.exists()) {
                    deleted = cloudBlockBlob.deleteIfExists();
                    if (deleted) {
                        log.info("Deleted file {} from container {}", blobName, containerName);
                    }
                } else {
                    deleted = true;
                    log.info("File {} (container {}) has already been deleted.", blobName, containerName);
                }
                if (deleted) {
                    envelope.setZipDeleted(true);
                    envelopeProcessor.saveEnvelope(envelope);
                    log.info("Marked envelope from file {} (container {}) as deleted", blobName, containerName);
                }
            } else {
                log.info("File {} from container {} not ready to be deleted yet.", blobName, containerName);
            }
        } catch (StorageException e) {
            log.error("Failed to delete file [{}] in container {}", blobName, containerName, e);
        } // Do not propagate exception as this blob has already been marked with a delete failure
    }

    private ZipFileProcessingResult processZipFileContent(
        ZipInputStream zis,
        String zipFilename,
        String containerName,
        String leaseId
    ) {
        try {
            ZipFileProcessor zipFileProcessor = new ZipFileProcessor(); // todo: inject
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                ZipVerifiers.ZipStreamWithSignature.fromKeyfile(zis, publicKeyDerFilename, zipFilename, containerName);

            ZipFileProcessingResult result = zipFileProcessor.process(
                zipWithSignature,
                ZipVerifiers.getPreprocessor(signatureAlg)
            );

            InputEnvelope envelope = envelopeProcessor.parseEnvelope(result.getMetadata(), zipFilename);

            EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                containerMappings.getMappings(), envelope, containerName
            );
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope);
            EnvelopeValidator.assertEnvelopeHasPdfs(envelope, result.getPdfs());
            EnvelopeValidator.assertDocumentControlNumbersAreUnique(envelope);

            envelopeProcessor.assertDidNotFailToUploadBefore(envelope.zipFileName, containerName);

            result.setEnvelope(envelopeProcessor.saveEnvelope(toDbEnvelope(envelope, containerName)));

            return result;
        } catch (InvalidEnvelopeException ex) {
            log.warn("Rejected file {} from container {} - invalid", zipFilename, containerName, ex);
            handleInvalidFileError(Event.FILE_VALIDATION_FAILURE, containerName, zipFilename, leaseId, ex);
            return null;
        } catch (DocSignatureFailureException ex) {
            log.warn("Rejected file {} from container {} - invalid signature", zipFilename, containerName, ex);
            handleInvalidFileError(Event.DOC_SIGNATURE_FAILURE, containerName, zipFilename, leaseId, ex);
            return null;
        } catch (PreviouslyFailedToUploadException ex) {
            log.warn("Rejected file {} from container {} - failed previously", zipFilename, containerName, ex);
            handleEventRelatedError(Event.DOC_UPLOAD_FAILURE, containerName, zipFilename, ex);
            return null;
        } catch (DataIntegrityViolationException ex) {
            // only report on constraint violations
            if (ex.getCause() instanceof ConstraintViolationException) {
                handleConstraintViolation(
                    containerName,
                    zipFilename,
                    leaseId,
                    (ConstraintViolationException) ex.getCause()
                );
            } else { // act same as before: `Exception` case
                log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
                handleEventRelatedError(Event.DOC_FAILURE, containerName, zipFilename, ex);
            }

            return null;
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, containerName, ex);
            handleEventRelatedError(Event.DOC_FAILURE, containerName, zipFilename, ex);
            return null;
        }
    }

    private boolean isReadyToBeProcessed(CloudBlockBlob blob) {
        java.util.Date cutoff = Date.from(Instant.now().minus(this.blobProcessingDelayInMinutes, ChronoUnit.MINUTES));
        return blob.getProperties().getLastModified().before(cutoff);
    }

    private boolean isReadyToBeDeleted(Envelope envelope) {
        return EnumSet.of(
            Status.PROCESSED,
            Status.NOTIFICATION_SENT,
            Status.COMPLETED
        ).contains(envelope.getStatus());
    }

    private void handleConstraintViolation(
        String containerName,
        String zipFilename,
        String leaseId,
        ConstraintViolationException exception
    ) {
        log.warn(
            "Rejected file {} from container {} - DB constraint violation {}",
            zipFilename,
            containerName,
            exception.getConstraintName(),
            exception
        );

        if (exception.getConstraintName().equals("scannable_item_dcn")) {
            handleInvalidFileError(
                Event.FILE_VALIDATION_FAILURE,
                containerName,
                zipFilename,
                leaseId,
                new DuplicateDocumentControlNumbersInEnvelopeException(
                    "Received envelope with 'document_control_number' already present in the system"
                )
            );
        } else {
            handleEventRelatedError(Event.DOC_FAILURE, containerName, zipFilename, exception);
        }
    }

    private void handleInvalidFileError(
        Event fileValidationFailure,
        String containerName,
        String zipFilename,
        String leaseId,
        Exception cause
    ) {
        Long eventId = handleEventRelatedError(fileValidationFailure, containerName, zipFilename, cause);

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
                null,
                null,
                errorCode,
                cause.getMessage()
            )
        );

        log.info(
            "Sent error notification for file {} in container {}. Message ID: {}",
            zipFilename,
            containerName,
            messageId
        );
    }

    private void logAbortedProcessingNotReadyFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - not ready yet.",
            zipFilename,
            containerName
        );
    }

    private void logAbortedProcessingNonExistingFile(String zipFilename, String containerName) {
        log.info(
            "Aborted processing of zip file {} from container {} - doesn't exist anymore.",
            zipFilename,
            containerName
        );
    }

}
