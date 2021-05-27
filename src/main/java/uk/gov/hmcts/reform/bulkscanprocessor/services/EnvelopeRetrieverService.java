package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@EnableConfigurationProperties(EnvelopeAccessProperties.class)
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

    public List<EnvelopeResponse> findByServiceAndStatus(String serviceName, Status status) {
        log.info("Fetch requested for envelopes for service {} and status {}", serviceName, status);

        String jurisdiction = envelopeAccessService.getReadJurisdictionForService(serviceName);
        var greaterThan = Instant.now().minus(24, ChronoUnit.HOURS);
        return EnvelopeResponseMapper.toEnvelopesResponse(
            status == null
                ?
                envelopeRepository.findByJurisdictionAndCreatedAtGreaterThan(
                    jurisdiction,
                    greaterThan
                )
                :
                envelopeRepository.findByJurisdictionAndStatusAndCreatedAtGreaterThan(
                    jurisdiction,
                    status,
                    greaterThan
                )
        );
    }

    public Optional<EnvelopeResponse> findById(String serviceName, UUID id) {
        String validJurisdiction = envelopeAccessService.getReadJurisdictionForService(serviceName);
        Optional<Envelope> envelope = envelopeRepository.findById(id);

        if (envelope.isPresent() && !Objects.equals(envelope.get().getJurisdiction(), validJurisdiction)) {
            throw new ForbiddenException("Service " + serviceName + " cannot read envelope " + id);
        } else {
            return envelope.map(EnvelopeResponseMapper::toEnvelopeResponse);
        }
    }
}
