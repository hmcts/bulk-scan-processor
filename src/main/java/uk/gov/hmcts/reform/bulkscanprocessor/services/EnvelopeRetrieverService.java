package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;

import java.util.List;
import java.util.Objects;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

@Service
@EnableConfigurationProperties(EnvelopeAccessProperties.class)
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeAccessProperties envelopeAccessProperties;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        EnvelopeAccessProperties envelopeAccessProperties
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeAccessProperties = envelopeAccessProperties;
    }

    public List<Envelope> getProcessedEnvelopesByJurisdiction(final String serviceName) {
        log.info("Fetch requested for envelopes for service {}", serviceName);

        String jurisdiction = getJurisdictionByServiceName(serviceName);

        log.info("Fetching all processed envelopes for service {} and jurisdiction {}", serviceName, jurisdiction);

        return envelopeRepository.findByJurisdictionAndStatus(jurisdiction, PROCESSED);
    }

    private String getJurisdictionByServiceName(String serviceName) {
        return envelopeAccessProperties
            .getMappings()
            .stream()
            .filter(m -> Objects.equals(m.getReadService(), serviceName))
            .findFirst()
            .map(m -> m.getJurisdiction())
            .orElseThrow(() ->
                new ServiceJuridictionConfigNotFoundException(
                    "No configuration mapping found for service " + serviceName
                )
            );
    }
}
