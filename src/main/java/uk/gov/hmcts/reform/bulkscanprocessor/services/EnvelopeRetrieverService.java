package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceJurisdictionMappingConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

@Service
@EnableConfigurationProperties(ServiceJurisdictionMappingConfig.class)
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;

    private final ServiceJurisdictionMappingConfig serviceJurisdictionMappingConfig;

    private final EnvelopeProcessor envelopeProcessor;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        ServiceJurisdictionMappingConfig serviceJurisdictionMappingConfig,
        EnvelopeProcessor envelopeProcessor
    ) {
        this.envelopeRepository = envelopeRepository;
        this.serviceJurisdictionMappingConfig = serviceJurisdictionMappingConfig;
        this.envelopeProcessor = envelopeProcessor;
    }

    public List<Envelope> getProcessedEnvelopesByJurisdiction(String serviceName) {
        String jurisdiction = getJurisdictionByServiceName(serviceName);

        log.info("Fetching all processed envelopes for service {} and jurisdiction {}", serviceName, jurisdiction);

        List<Envelope> processedEnvelopes =
            envelopeRepository.findByJurisdictionAndStatus(
                jurisdiction,
                Event.DOC_PROCESSED
            );

        updateEnvelopeStatusAndCreateEvent(processedEnvelopes);

        return processedEnvelopes;
    }

    private void updateEnvelopeStatusAndCreateEvent(List<Envelope> processedEnvelopes) {
        processedEnvelopes.forEach(envelopeProcessor::markAsConsumed);
        envelopeRepository.saveAll(processedEnvelopes);
    }

    private String getJurisdictionByServiceName(String serviceName) {
        String jurisdiction = serviceJurisdictionMappingConfig
            .getServicesJurisdiction()
            .getOrDefault(serviceName, null);

        if (jurisdiction == null) {
            throw new ServiceJuridictionConfigNotFoundException(
                "No configuration mapping found for service " + serviceName
            );
        }
        return jurisdiction;
    }
}
