package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.util.List;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.HOURS;

@Service
public class IncompleteEnvelopesService {

    private final EnvelopeRepository envelopeRepository;

    public IncompleteEnvelopesService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    public List<Envelope> getIncompleteEnvelopes(int staleTimeHr) {
        return envelopeRepository.getIncompleteEnvelopesBefore(now().minus(staleTimeHr, HOURS));
    }
}
