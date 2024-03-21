package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;

import java.util.Objects;

/**
 * Service to access the configuration for envelope access.
 */
@Service
public class EnvelopeAccessService {

    private final EnvelopeAccessProperties access;

    /**
     * Constructor for EnvelopeAccessService.
     * @param accessProps EnvelopeAccessProperties
     */
    public EnvelopeAccessService(EnvelopeAccessProperties accessProps) {
        this.access = accessProps;
    }

    /**
     * Returns the name of jurisdiction from which given service can read envelopes.
     * @param serviceName The name of the service
     */
    public String getReadJurisdictionForService(String serviceName) {
        return access
            .getMappings()
            .stream()
            .filter(m -> Objects.equals(m.getReadService(), serviceName))
            .findFirst()
            .map(EnvelopeAccessProperties.Mapping::getJurisdiction)
            .orElseThrow(() ->
                new ServiceJuridictionConfigNotFoundException(
                    "No configuration mapping found for service " + serviceName
                )
            );
    }
}
