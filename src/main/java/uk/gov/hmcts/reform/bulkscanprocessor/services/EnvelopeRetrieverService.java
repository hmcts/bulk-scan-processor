package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.List;

@Service
@EnableConfigurationProperties(EnvelopeAccessProperties.class)
public class EnvelopeRetrieverService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeRetrieverService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeAccessService envelopeAccessService;
    private final EnvelopeResponseMapper envelopeResponseMapper;

    public EnvelopeRetrieverService(
        EnvelopeRepository envelopeRepository,
        EnvelopeAccessService envelopeAccessService
    ) {
        this.envelopeRepository = envelopeRepository;
        this.envelopeAccessService = envelopeAccessService;
        this.envelopeResponseMapper = new EnvelopeResponseMapper();
    }

    public List<EnvelopeResponse> findByServiceAndStatus(String serviceName, Status status) {
        log.info("Fetch requested for envelopes for service {} and status {}", serviceName, status);

        String jurisdiction = envelopeAccessService.getReadJurisdictionForService(serviceName);

        return envelopeResponseMapper.toEnvelopesResponse(
            status == null
            ? envelopeRepository.findByJurisdiction(jurisdiction)
            : envelopeRepository.findByJurisdictionAndStatus(jurisdiction, status)
        );
    }

}
