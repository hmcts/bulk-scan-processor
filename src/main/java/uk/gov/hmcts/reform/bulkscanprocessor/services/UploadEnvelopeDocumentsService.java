package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocSignatureFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;

/**
 * Service responsible to upload envelopes ended in state after main processor task.
 * <p></p>
 * {@link uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask}
 * <p></p>
 * All the validations have been accomplished and {@link Envelope} saved in DB.
 * Therefore no need to go through the same set of validation steps/rules and just re-extract zip file and upload.
 * In case some {@link StorageException} or {@link IOException} experienced during zip extraction
 * the envelope state will not be changed and left for a retry on the next run.
 * {@link Event#DOC_UPLOAD_FAILURE} can be treated as stuck envelope and be re-uploaded by same service here.
 * There is an upload retry counter/limit which may be used as well
 * Review ^ last sentence and, perhaps, incorporate such possibility here.
 */
@Service
public class UploadEnvelopeDocumentsService {

    private static final Logger log = getLogger(UploadEnvelopeDocumentsService.class);

    private final long coolDownPeriod;
    private final EnvelopeRepository envelopeRepository;
    private final BlobManager blobManager;
    private final ZipFileProcessor zipFileProcessor;
    private final ProcessEventRepository eventRepository;
    private final DocumentProcessor documentProcessor;
    private final EnvelopeProcessor envelopeProcessor;

    public UploadEnvelopeDocumentsService(
        // only process envelopes after cool down period
        // can be removed once uploading feature is removed from main job
        @Value("${scheduling.task.upload-documents.cool-down-minutes}") long coolDownPeriod,
        EnvelopeRepository envelopeRepository,
        BlobManager blobManager,
        ZipFileProcessor zipFileProcessor,
        ProcessEventRepository eventRepository,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor
    ) {
        this.coolDownPeriod = coolDownPeriod;
        this.envelopeRepository = envelopeRepository;
        this.blobManager = blobManager;
        this.zipFileProcessor = zipFileProcessor;
        this.eventRepository = eventRepository;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
    }

    public void processEnvelopes() {
        envelopeRepository
            .findByStatus(CREATED)
            .stream()
            // can be moved to query instead. but it won't be needed after upload is removed from main task
            .filter(envelope -> envelope.getCreatedAt().isBefore(now().minus(coolDownPeriod, MINUTES)))
            .collect(groupingBy(Envelope::getContainer))
            .forEach(this::processByContainer);
    }

    private void processByContainer(String containerName, List<Envelope> envelopes) {
        log.info("Processing envelopes in {} container. Envelopes found: {}", containerName, envelopes.size());

        try {
            CloudBlobContainer blobContainer = blobManager.getContainer(containerName);

            envelopes.forEach(envelope -> {
                Optional<ZipFileProcessingResult> resultOption = getBlobInputStream(
                    blobContainer,
                    envelope.getZipFileName()
                ).flatMap(inputStream ->
                    processInputStream(inputStream, containerName, envelope.getZipFileName())
                );

                boolean isUploaded = resultOption
                    .map(result -> uploadParsedZipFileName(envelope, result.getPdfs()))
                    .orElse(false);

                // only update envelope state if no errors occurred during zip file processing
                if (resultOption.isPresent()) {
                    envelopeProcessor.handleEvent(envelope, isUploaded ? DOC_UPLOADED : DOC_UPLOAD_FAILURE);
                }
            });
        } catch (URISyntaxException | StorageException exception) {
            log.error("Unable to get client for {} container", containerName, exception);
        }
    }

    private Optional<BlobInputStream> getBlobInputStream(CloudBlobContainer blobContainer, String zipFileName) {
        try {
            return Optional.of(
                blobContainer
                    .getBlockBlobReference(zipFileName)
                    .openInputStream()
            );
        } catch (URISyntaxException | StorageException exception) {
            log.error(
                "Unable to get blob input stream. Container: {}, blob: {}",
                blobContainer.getName(),
                zipFileName
            );

            return Optional.empty();
        }
    }

    private Optional<ZipFileProcessingResult> processInputStream(
        BlobInputStream blobInputStream,
        String containerName,
        String zipFileName
    ) {
        try (ZipInputStream zis = new ZipInputStream(blobInputStream)) {
            return Optional.of(
                zipFileProcessor.process(zis, containerName, zipFileName)
            );
        } catch (DocSignatureFailureException exception) {
            log.warn(
                "Rejecting blob - invalid signature. File: {}, Container: {}",
                zipFileName,
                containerName,
                exception
            );

            handleEventRelatedError(DOC_SIGNATURE_FAILURE, containerName, zipFileName, exception.getMessage());

            blobManager.tryMoveFileToRejectedContainer(zipFileName, containerName, null);
        } catch (IOException exception) {
            log.error("Failure reading zip. File: {}, Container: {}", zipFileName, containerName, exception);

            handleEventRelatedError(DOC_FAILURE, containerName, zipFileName, exception.getMessage());
        }

        return Optional.empty();
    }

    private boolean uploadParsedZipFileName(Envelope envelope, List<Pdf> pdfs) {
        try {
            documentProcessor.uploadPdfFiles(pdfs, envelope.getScannableItems());

            log.info("Uploaded pdfs. File {}, container: {}", envelope.getZipFileName(), envelope.getContainer());

            return true;
        } catch (Exception exception) {
            log.error(
                "Failed to upload PDF files to Document Management. File: {}, Container: {}",
                envelope.getZipFileName(),
                envelope.getContainer(),
                exception
            );

            envelope.setUploadFailureCount(envelope.getUploadFailureCount() + 1);
            envelopeRepository.saveAndFlush(envelope);
            // no need to create event. envelopeProcessor::handleEvent does that

            return false;
        }
    }

    private void handleEventRelatedError(Event event, String containerName, String zipFileName, String reason) {
        ProcessEvent processEvent = new ProcessEvent(
            containerName,
            zipFileName,
            event
        );

        processEvent.setReason(reason);
        eventRepository.saveAndFlush(processEvent);

        log.info(
            "Zip {} from {} marked as {}",
            processEvent.getZipFileName(),
            processEvent.getContainer(),
            processEvent.getEvent()
        );
    }
}
