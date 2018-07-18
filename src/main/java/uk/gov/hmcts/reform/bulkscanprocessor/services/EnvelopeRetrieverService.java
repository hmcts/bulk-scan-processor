package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceJurisdictionMappingConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;

import java.util.List;

@Service
@EnableConfigurationProperties(ServiceJurisdictionMappingConfig.class)
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;

    private final ServiceJurisdictionMappingConfig serviceJurisdictionMappingConfig;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        ServiceJurisdictionMappingConfig serviceJurisdictionMappingConfig
    ) {
        this.envelopeRepository = envelopeRepository;
        this.serviceJurisdictionMappingConfig = serviceJurisdictionMappingConfig;
    }

    public List<Envelope> getAllEnvelopesForJurisdiction(String serviceName) {
        String jurisdiction = getJurisdictionByServiceName(serviceName);

        log.info("Fetching all envelopes for service {} and jurisdiction {}", serviceName, jurisdiction);

        return envelopeRepository.findByJurisdiction(jurisdiction);
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
