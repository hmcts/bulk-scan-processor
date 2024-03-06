package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.UUID;
import jakarta.transaction.Transactional;

@Service
public class EnvelopeFinaliserService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeFinaliserService.class);

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
    public void finaliseEnvelope(UUID envelopeId, String ccdId, String envelopeCcdAction) {
        log.info(
            "Finalising envelope, envelopeId: {}, ccdId: {}, envelopeCcdAction: {}",
            envelopeId,
            ccdId,
            envelopeCcdAction
        );

        Envelope envelope = findEnvelope(envelopeId);

        envelope.getScannableItems().forEach(item -> {
            item.setOcrData(null);
            item.setOcrValidationWarnings(null);
        });
        envelope.setStatus(Status.COMPLETED);
        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(envelopeCcdAction);

        envelopeRepository.saveAndFlush(envelope);

        log.info(
            "Saved envelope, envelopeId: {}, ccdId: {}, envelopeCcdAction: {}, status: {}",
            envelope.getId(),
            envelope.getCcdId(),
            envelope.getEnvelopeCcdAction(),
            envelope.getStatus()
        );

        processEventRepository.saveAndFlush(
            new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), Event.COMPLETED)
        );

        log.info(
            "Saved processEvent, container: {}, zipFileName: {}, event: {}",
            envelope.getContainer(),
            envelope.getZipFileName(),
            Event.COMPLETED
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
