package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeBeingProcessedException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeProcessedException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.UUID;

@Service
public class EnvelopeReprocessService {
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;

    public EnvelopeReprocessService(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
    }

    @Transactional
    public void reprocessEnvelope(String envelopeId) {
        Envelope envelope = envelopeRepository.findById(UUID.fromString(envelopeId))
            .orElseThrow(
                () -> new EnvelopeNotFoundException("Envelope with id " + envelopeId + " not found")
            );

        validateEnvelopeState(envelope);

        createEvent(envelope);

        envelope.setStatus(Status.UPLOADED);
        envelopeRepository.save(envelope);
    }

    private void createEvent(Envelope envelope) {
        ProcessEvent event = new ProcessEvent(
            envelope.getContainer(),
            envelope.getZipFileName(),
            Event.ONLINE_STATUS_CHANGE
        );
        event.setReason("Moved to UPLOADED status to reprocess the envelope");
        processEventRepository.save(event);
    }

    private void validateEnvelopeState(Envelope envelope) {
        if (envelope.getCcdId() != null) {
            throw new EnvelopeProcessedException(
                "Envelope with id " + envelope.getId() + " has already been processed"
            );
        }
        if (envelope.getStatus() == Status.UPLOADED) {
            throw new EnvelopeBeingProcessedException(
                "Envelope with id " + envelope.getId() + " is being processed"
            );
        }
    }
}
