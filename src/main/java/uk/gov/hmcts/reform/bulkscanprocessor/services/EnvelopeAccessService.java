package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;

import java.util.Objects;

@Service
public class EnvelopeAccessService {

    private final EnvelopeAccessProperties access;

    public EnvelopeAccessService(EnvelopeAccessProperties accessProps) {
        this.access = accessProps;
    }

    /**
     * Returns the name of jurisdiction from which given service can read envelopes.
     */
    public String getReadJurisdictionForService(String serviceName) {
        return access
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
