package uk.gov.hmcts.reform.bulkscanprocessor.services;

import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceConfigNotFoundException;

import java.util.Objects;
import java.util.Optional;

public class EnvelopeAccessService {

    private final EnvelopeAccessProperties access;

    public EnvelopeAccessService(EnvelopeAccessProperties access) {
        this.access = access;
    }

    /**
     * Checks whether envelope from given jurisdiction can be update by given service.
     * Throws an exception if service is not allowed to make and update or configuration for the jurisdiction is not found.
     */
    public void assertCanUpdate(String envelopeJurisdiction, String serviceName) {
        Optional<String> serviceThanCanUpdateEnvelope =
            access
                .getMappings()
                .stream()
                .filter(m -> Objects.equals(m.getJurisdiction(), envelopeJurisdiction))
                .findFirst()
                .map(m -> m.getUpdateService());

        if (serviceThanCanUpdateEnvelope.isPresent()) {
            if (serviceThanCanUpdateEnvelope.get() != serviceName) {
                throw new ForbiddenException(
                    "Service " + serviceName + " cannot update envelopes in jurisdiction " + envelopeJurisdiction
                );
            }
        } else {
            throw new ServiceConfigNotFoundException(
                "No service configuration found to update envelopes in jurisdiction: " + envelopeJurisdiction
            );
        }
    }
}
