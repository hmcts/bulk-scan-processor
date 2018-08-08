package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.List;
import java.util.Objects;

@Service
@EnableConfigurationProperties(EnvelopeAccessProperties.class)
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeAccessProperties envelopeAccessProperties;
    private final EnvelopeResponseMapper envelopeResponseMapper;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        EnvelopeAccessProperties envelopeAccessProperties
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeAccessProperties = envelopeAccessProperties;
        this.envelopeResponseMapper = new EnvelopeResponseMapper();
    }

    public List<EnvelopeResponse> findByServiceAndStatus(String serviceName, Status status) {
        log.info("Fetch requested for envelopes for service {} and status {}", serviceName, status);

        String jurisdiction = getJurisdictionByServiceName(serviceName);

        return envelopeResponseMapper.toEnvelopesResponse(
            status == null
            ? envelopeRepository.findByJurisdiction(jurisdiction)
            : envelopeRepository.findByJurisdictionAndStatus(jurisdiction, status)
        );
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
