package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeClassificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotCompletedOrStaleException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotInInconsistentStateException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeProcessedInCcdException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Comparator.naturalOrder;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ABORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_RETRIGGER_PROCESSING;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_STATUS_CHANGE;

/**
 * Service to perform actions on envelopes.
 */
@Service
public class EnvelopeActionService {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeActionService.class);

    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;
    private final long notificationTimeoutHr;

    private static final String CASE_REFERENCE_NOT_PRESENT = "(NOT PRESENT)";

    /**
     * Constructor for EnvelopeActionService.
     * @param envelopeRepository EnvelopeRepository
     * @param processEventRepository ProcessEventRepository
     * @param notificationTimeoutHr long
     */
    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository,
        @Value("${notification-stale-timeout-hr}") long notificationTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
        this.notificationTimeoutHr = notificationTimeoutHr;
    }

    /**
     * Reprocesses the envelope by envelope id.
     * @param envelopeId The envelope id
     */
    @Transactional
    public void reprocessEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException(getErrorMessage(envelopeId, "not found"))
            );

        validateEnvelopeStateForReprocess(envelope);

        createEvent(
            envelope,
            MANUAL_RETRIGGER_PROCESSING,
            "Moved to UPLOADED status to reprocess the envelope"
        );

        envelope.setStatus(UPLOADED);
        envelopeRepository.save(envelope);

        log.info("Envelope {} status changed to UPLOADED. Case reference: {}", envelope.getZipFileName(),
                 Optional.ofNullable(envelope.getCaseNumber()).orElse(CASE_REFERENCE_NOT_PRESENT));
    }

    /**
     * Moves the envelope to completed status by envelope id.
     * @param envelopeId The envelope id
     */
    @Transactional
    public void moveEnvelopeToCompleted(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException(getErrorMessage(envelopeId, "not found"))
            );

        validateEnvelopeIsInInconsistentState(envelope);

        createEvent(
            envelope,
            MANUAL_STATUS_CHANGE,
            "Moved to COMPLETED status to fix inconsistent state caused by race conditions"
        );

        envelope.setStatus(COMPLETED);
        envelopeRepository.save(envelope);

        log.info("Envelope {} status changed to COMPLETED. Case Reference: {}", envelope.getZipFileName(),
                 Optional.ofNullable(envelope.getCaseNumber()).orElse(CASE_REFERENCE_NOT_PRESENT));
    }

    /**
     * Moves the envelope to aborted status by envelope id.
     * @param envelopeId The envelope id
     */
    @Transactional
    public void moveEnvelopeToAborted(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException(getErrorMessage(envelopeId, "not found"))
            );

        validateEnvelopeStateForAbort(envelope);

        createEvent(
            envelope,
            MANUAL_STATUS_CHANGE,
            "Moved to ABORTED status to fix inconsistent state unresolved by the service"
        );

        envelope.setStatus(ABORTED);
        envelopeRepository.save(envelope);

        log.info("Envelope {} status changed to ABORTED. Case reference: {}", envelope.getZipFileName(),
                 Optional.ofNullable(envelope.getCaseNumber()).orElse(CASE_REFERENCE_NOT_PRESENT));
    }

    /**
     * Updates the envelope classification to EXCEPTION and reprocesses the envelope by envelope id.
     * @param envelopeId The envelope id
     */
    @Transactional
    public void updateClassificationAndReprocessEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException(getErrorMessage(envelopeId, "not found"))
            );

        validateEnvelopeClassification(envelopeId, envelope.getClassification());
        validateEnvelopeStateForReprocess(envelope);

        createEvent(
            envelope,
            MANUAL_RETRIGGER_PROCESSING,
            "Updated envelope classification to EXCEPTION and status to UPLOADED "
                + "to create Exception Record for the envelope"
        );

        envelopeRepository.updateEnvelopeClassificationAndStatus(envelopeId, envelope.getContainer());

        log.info(
            "Updated Envelope {} classification to 'EXCEPTION' and status to 'UPLOADED'", envelope.getZipFileName());
    }

    /**
     * Creates an event for the envelope.
     * @param envelope The envelope
     * @param event The event
     * @param reason The reason
     */
    private void createEvent(Envelope envelope, Event event, String reason) {
        ProcessEvent processEvent = new ProcessEvent(
            envelope.getContainer(),
            envelope.getZipFileName(),
            event
        );
        processEvent.setReason(reason);
        processEventRepository.save(processEvent);
    }

    /**
     * Validates the envelope state for reprocess.
     * @param envelope The envelope
     */
    private void validateEnvelopeStateForReprocess(Envelope envelope) {
        validateNotProcessedInCcd(envelope);

        if (envelope.getStatus() != COMPLETED && envelope.getStatus() != ABORTED && !isStale(envelope)) {
            throw new EnvelopeNotCompletedOrStaleException(
                    getErrorMessage(envelope.getId(), "is not completed, aborted or stale")
            );
        }
    }

    /**
     * Validates the envelope classification.
     * @param id The envelope id
     * @param classification The classification
     */
    private void validateEnvelopeClassification(UUID id, Classification classification) {
        if (classification != Classification.SUPPLEMENTARY_EVIDENCE) {
            throw new EnvelopeClassificationException(
                getErrorMessage(id, "does not have SUPPLEMENTARY_EVIDENCE classification")
            );
        }
    }

    /**
     * Validates the envelope state for abort.
     * @param envelope The envelope
     */
    private void validateEnvelopeStateForAbort(Envelope envelope) {
        validateNotProcessedInCcd(envelope);

        if (envelope.getStatus() != COMPLETED && !isStale(envelope)) {
            throw new EnvelopeNotCompletedOrStaleException(
                    getErrorMessage(envelope.getId(), "is not completed or stale")
            );
        }
    }

    /**
     * Validates that the envelope is not processed in CCD.
     * @param envelope The envelope
     */
    private void validateNotProcessedInCcd(Envelope envelope) {
        if (envelope.getCcdId() != null) {
            throw new EnvelopeProcessedInCcdException(
                    getErrorMessage(envelope.getId(), "has already been processed in CCD")
            );
        }
    }

    /**
     * Validates that the envelope is in inconsistent state.
     * @param envelope The envelope
     */
    private void validateEnvelopeIsInInconsistentState(Envelope envelope) {
        if (envelope.getStatus() == COMPLETED || envelope.getStatus() == ABORTED) {
            throw new EnvelopeNotInInconsistentStateException(
                    getErrorMessage(envelope.getId(), "is not in inconsistent state")
            );
        }

        if (processEventRepository.findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName())
            .stream()
            .filter(ev -> ev.getEvent() == Event.COMPLETED)
            .findFirst()
            .isEmpty()
        ) {
            throw new EnvelopeNotInInconsistentStateException(
                    getErrorMessage(envelope.getId(), "does not have COMPLETED event")
            );
        }
    }

    /**
     * Checks if the envelope is stale.
     * @param envelope The envelope
     * @return true if the envelope is stale
     */
    private boolean isStale(Envelope envelope) {
        log.info("Envelope {} has status {}", envelope.getId(), envelope.getStatus());
        if (envelope.getStatus() != Status.NOTIFICATION_SENT) {
            return false;
        }

        Instant lastEventTimeStamp = processEventRepository
            .findByZipFileNameOrderByCreatedAtDesc(envelope.getZipFileName())
            .stream()
            .map(ProcessEvent::getCreatedAt)
            .max(naturalOrder())
            .orElseThrow(); // no events for the envelope is normally impossible
        log.info(
                "Envelope {} lastEventTimeStamp {} is {} hours before now {}, notificationTimeoutHr is {}",
                envelope.getId(),
                lastEventTimeStamp,
                between(lastEventTimeStamp, now()).toHours(),
                Instant.now(),
                notificationTimeoutHr
        );
        return between(lastEventTimeStamp, now()).toHours() > notificationTimeoutHr;
    }

    /**
     * Returns the error message for the envelope.
     * @param id The envelope id
     * @param msg The message
     * @return The error message
     */
    private String getErrorMessage(UUID id, String msg) {
        return "Envelope with id " + id + " " + msg;
    }
}
