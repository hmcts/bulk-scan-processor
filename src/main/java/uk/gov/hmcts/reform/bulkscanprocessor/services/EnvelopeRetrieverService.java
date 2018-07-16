package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.util.List;

@Service
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;

    public EnvelopeRetrieverService(EnvelopeRepository envelopeRepository) {
        this.envelopeRepository = envelopeRepository;
    }

    public List<Envelope> getAllEnvelopes() {
        log.info("Fetching all envelopes");

        return envelopeRepository.findAll();
    }
}
