package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumberException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeSchemaException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

/**
 * Processes the envelope.
 */
@Component
public class EnvelopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeProcessor.class);

    private final MetafileJsonValidator schemaValidator;
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;

    /**
     * Constructor for the EnvelopeProcessor.
     * @param schemaValidator The schema validator
     * @param envelopeRepository The envelope repository
     * @param processEventRepository The process event repository
     */
    public EnvelopeProcessor(
        MetafileJsonValidator schemaValidator,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository
    ) {
        this.schemaValidator = schemaValidator;
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
    }

    /**
     * Parses the envelope.
     * @param metadataStream The metadata stream
     * @param zipFileName The zip file name
     * @return The input envelope
     * @throws IOException If an I/O error occurs
     * @throws ProcessingException If a processing exception occurs
     */
    public InputEnvelope parseEnvelope(
        byte[] metadataStream,
        String zipFileName
    ) throws IOException, ProcessingException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }

        try {
            schemaValidator.validate(metadataStream, zipFileName);

            return schemaValidator.parseMetafile(metadataStream);
        } catch (JsonParseException | OcrDataParseException exception) {
            // invalid json files should also be reported to provider
            throw new InvalidEnvelopeSchemaException("Error occurred while parsing metafile", exception);
        }
    }

    /**
     * Assert zip file did not fail to be uploaded in the past.
     * Throws exception otherwise.
     * @param zipFileName The zip file name
     * @param containerName The container name
     */
    public void assertDidNotFailToUploadBefore(String zipFileName, String containerName) {
        List<Envelope> envelopes = envelopeRepository.findRecentEnvelopes(
            containerName,
            zipFileName,
            UPLOAD_FAILURE,
            PageRequest.of(0, 1)
        );

        if (envelopes.size() == 1) {
            Envelope failedEnvelope = envelopes.get(0);

            throw new PreviouslyFailedToUploadException(
                String.format(
                    "Envelope %s created at %s is already marked as failed to upload. Skipping",
                    failedEnvelope.getId(),
                    failedEnvelope.getCreatedAt()
                )
            );
        }
    }

    /**
     * Check blob did not fail to be deleted before. This means that
     * processing is complete and an envelope has already been created as
     * blob deletion is the last processing step.
     * @param zipFileName The zip file name
     * @param container The container name
     */
    public Envelope getEnvelopeByFileAndContainer(String container, String zipFileName) {
        return envelopeRepository.findFirstByZipFileNameAndContainerOrderByCreatedAtDesc(
            zipFileName,
            container
        );
    }

    /**
     * Saves the envelope.
     * @param envelope The envelope
     */
    public void saveEnvelope(Envelope envelope) {
        try {
            Envelope dbEnvelope = envelopeRepository.saveAndFlush(envelope);

            log.info(
                "Envelope successfully saved in database. Jurisdiction: {}, File name: {}, Envelope ID: {}",
                envelope.getJurisdiction(),
                envelope.getZipFileName(),
                dbEnvelope.getId()
            );
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException) {
                evaluateConstraintException((ConstraintViolationException) exception.getCause());
            }

            // or throw anyway (same behaviour as before)
            throw exception;
        }
    }

    /**
     * Marks the envelope as upload failure.
     * @param envelope The envelope
     */
    public void markAsUploadFailure(Envelope envelope) {
        envelope.setUploadFailureCount(envelope.getUploadFailureCount() + 1);
        envelope.setStatus(UPLOAD_FAILURE);

        envelopeRepository.saveAndFlush(envelope);

        log.info("Envelope {} status changed to UPLOAD_FAILURE", envelope.getZipFileName());
    }

    /**
     * Creates an event.
     * @param event The event
     * @param containerName The container name
     * @param zipFileName The zip file name
     * @param reason The reason
     * @return The event ID
     */
    public long createEvent(Event event, String containerName, String zipFileName, String reason, UUID envelopeId) {
        ProcessEvent processEvent = new ProcessEvent(
            containerName,
            zipFileName,
            event
        );

        processEvent.setReason(reason);
        long eventId = processEventRepository.saveAndFlush(processEvent).getId();

        log.info(
            "Zip {} from {} marked as {}. Envelope ID: {}",
            processEvent.getZipFileName(),
            processEvent.getContainer(),
            processEvent.getEvent(),
            envelopeId
        );

        return eventId;
    }

    /**
     * Handles the event.
     * @param envelope The envelope
     * @param event The event
     */
    public void handleEvent(Envelope envelope, Event event) {
        processEventRepository.saveAndFlush(
            new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), event)
        );

        Status.fromEvent(event).ifPresent(status -> {
            envelope.setStatus(status);
            envelopeRepository.saveAndFlush(envelope);

            log.info("Envelope {} status changed to {}", envelope.getZipFileName(), status.name());
        });
    }

    /**
     * Evaluates the constraint exception.
     * @param exception The exception
     */
    private void evaluateConstraintException(ConstraintViolationException exception) {
        // for further constraint issues can be replaced with switch statement
        if (exception.getConstraintName().equals("scannable_item_dcn")) {
            throw new DuplicateDocumentControlNumberException(
                "Received envelope with 'document_control_number' already present in the system",
                exception
            );
        }
    }
}
