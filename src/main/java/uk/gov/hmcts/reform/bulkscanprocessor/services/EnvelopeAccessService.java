package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import java.util.List;
import java.util.Objects;

@Service
public class EnvelopeAccessService {

    private final List<Mapping> mappings;

    public EnvelopeAccessService(EnvelopeAccessProperties accessProps) {
        this.mappings = accessProps.getMappings();
    }

    /**
     * Checks whether envelope from given jurisdiction can be updated by given service.
     * Throws an exception if service is not allowed to make an update
     * or configuration for the jurisdiction is not found.
     */
    public void assertCanUpdate(String envelopeJurisdiction, String serviceName) {
        String serviceThanCanUpdateEnvelope =
            mappings
                .stream()
                .filter(m -> Objects.equals(m.getJurisdiction(), envelopeJurisdiction))
                .findFirst()
                .map(Mapping::getUpdateService)
                .orElseThrow(() -> new ServiceConfigNotFoundException(
                    "No service configuration found to update envelopes in jurisdiction: " + envelopeJurisdiction
                ));

        if (!serviceThanCanUpdateEnvelope.equals(serviceName)) {
            throw new ForbiddenException(
                "Service " + serviceName + " cannot update envelopes in jurisdiction " + envelopeJurisdiction
            );
        }
    }
}
