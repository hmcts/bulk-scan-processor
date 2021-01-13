package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;

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
    }
}
