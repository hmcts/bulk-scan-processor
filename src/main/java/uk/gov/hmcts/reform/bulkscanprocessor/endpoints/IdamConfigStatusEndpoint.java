package uk.gov.hmcts.reform.bulkscanprocessor.endpoints;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.AuthenticationChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.JurisdictionConfigurationStatus;

import java.util.List;

/**
 * Endpoint to check the status of IDAM configuration for all jurisdictions or for a specific jurisdiction.
 */
@Component
@Endpoint(id = "idam-config-status")
public class IdamConfigStatusEndpoint {

    private final AuthenticationChecker authenticationChecker;

    /**
     * Creates a new instance of the endpoint.
     * @param authenticationChecker the checker to use
     */
    public IdamConfigStatusEndpoint(AuthenticationChecker authenticationChecker) {
        this.authenticationChecker = authenticationChecker;
    }

    /**
     * Returns the status of IDAM configuration for all jurisdictions.
     * @return List of status of IDAM configuration for all jurisdictions
     */
    @ReadOperation
    public List<JurisdictionConfigurationStatus> jurisdictions() {
        return authenticationChecker.checkSignInForAllJurisdictions();
    }

    /**
     * Returns the status of IDAM configuration for a specific jurisdiction.
     * @param jurisdiction the jurisdiction to check
     * @return the status of IDAM configuration for the specified jurisdiction
     */
    @ReadOperation
    public JurisdictionConfigurationStatus jurisdiction(@Selector String jurisdiction) {
        return authenticationChecker.checkSignInForJurisdiction(jurisdiction);
    }
}
