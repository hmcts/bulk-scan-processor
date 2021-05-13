package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotCompletedOrStaleException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotInInconsistentStateException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeProcessedInCcdException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.Comparator.naturalOrder;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_RETRIGGER_PROCESSING;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.MANUAL_STATUS_CHANGE;

@Service
public class EnvelopeActionService {
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;
    private final long notificationTimeoutHr;

    public EnvelopeActionService(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository,
        @Value("${notification-stale-timeout-hr}") long notificationTimeoutHr
    ) {
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
        this.notificationTimeoutHr = notificationTimeoutHr;
    }

    @Transactional
    public void reprocessEnvelope(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );

        validateEnvelopeState(envelope);

        createEvent(
            envelope,
            MANUAL_RETRIGGER_PROCESSING,
            "Moved to UPLOADED status to reprocess the envelope"
        );

        envelope.setStatus(UPLOADED);
        envelopeRepository.save(envelope);
    }

    @Transactional
    public void moveEnvelopeToCompleted(UUID envelopeId) {
        Envelope envelope = envelopeRepository.findById(envelopeId)
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );

        validateEnvelopeIsInInconsistentState(envelope);

        createEvent(
            envelope,
            MANUAL_STATUS_CHANGE,
            "Moved to COMPLETED status to fix inconsistent state caused by race conditions"
        );

        envelope.setStatus(COMPLETED);
        envelopeRepository.save(envelope);
    }

    private void createEvent(Envelope envelope, Event event, String reason) {
        ProcessEvent processEvent = new ProcessEvent(
            envelope.getContainer(),
            envelope.getZipFileName(),
            event
        );
        processEvent.setReason(reason);
        processEventRepository.save(processEvent);
    }

    private void validateEnvelopeState(Envelope envelope) {
        if (envelope.getCcdId() != null) {
            throw new EnvelopeProcessedInCcdException(
                "Envelope with id " + envelope.getId() + " has already been processed in CCD"
            );
        }

        if (envelope.getStatus() != COMPLETED && !isStale(envelope)) {
            throw new EnvelopeNotCompletedOrStaleException(
                "Envelope with id " + envelope.getId() + " is not completed or stale"
            );
        }
    }

    private void validateEnvelopeIsInInconsistentState(Envelope envelope) {
        if (envelope.getStatus() == COMPLETED) {
            throw new EnvelopeNotInInconsistentStateException(
                "Envelope with id " + envelope.getId() + " is not in inconsistent state"
            );
        }

        if (processEventRepository.findByZipFileName(envelope.getZipFileName())
            .stream()
            .filter(ev -> ev.getEvent() == Event.COMPLETED)
            .findFirst()
            .isEmpty()
        ) {
            throw new EnvelopeNotInInconsistentStateException(
                "Envelope with id " + envelope.getId() + " does not have COMPLETED event"
            );
        }
    }

    private boolean isStale(Envelope envelope) {
        if (envelope.getStatus() != Status.NOTIFICATION_SENT) {
            return false;
        }

        Instant lastEventTimeStamp = processEventRepository
            .findByZipFileName(envelope.getZipFileName())
            .stream()
            .map(ProcessEvent::getCreatedAt)
            .max(naturalOrder())
            .orElseThrow(); // no events for the envelope is normally impossible
        return between(lastEventTimeStamp, now()).toHours() > notificationTimeoutHr;
    }
}
