package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@Service
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeAccessService envelopeAccessService;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        EnvelopeAccessService envelopeAccessService
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeAccessService = envelopeAccessService;
    }

    public List<Envelope> getProcessedEnvelopesByJurisdiction(final String serviceName) {
        log.info("Fetch requested for envelopes for service {}", serviceName);

        String jurisdiction = envelopeAccessService.getJurisdictionByServiceName(serviceName);

        log.info("Fetching all processed envelopes for service {} and jurisdiction {}", serviceName, jurisdiction);

        return envelopeRepository.findByJurisdictionAndStatus(jurisdiction, PROCESSED);
    }
}
