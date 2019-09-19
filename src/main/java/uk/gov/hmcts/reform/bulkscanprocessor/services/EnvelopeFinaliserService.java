package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import javax.transaction.Transactional;
import java.util.UUID;

@Service
public class EnvelopeFinaliserService {

    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;

    public EnvelopeFinaliserService(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository
    ) {
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
    }

    @Transactional
    public void finaliseEnvelope(UUID envelopeId) {
        Envelope envelope = findEnvelope(envelopeId);

        envelope.getScannableItems().forEach(item -> {
            item.setOcrData(null);
            item.setOcrValidationWarnings(null);
        });
        envelope.setStatus(Status.COMPLETED);
        envelopeRepository.saveAndFlush(envelope);

        processEventRepository.saveAndFlush(
            new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), Event.COMPLETED)
        );
    }

    private Envelope findEnvelope(UUID envelopeId) {
        return envelopeRepository.findById(envelopeId)
            .orElseThrow(() ->
                new EnvelopeNotFoundException(
                    String.format("Envelope with ID %s couldn't be found", envelopeId)
                )
            );
    }
}
