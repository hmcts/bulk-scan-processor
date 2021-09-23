package uk.gov.hmcts.reform.bulkscanprocessor.endpoints;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.AuthenticationChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.JurisdictionConfigurationStatus;

import java.util.List;

@Component
@Endpoint(id = "idam-config-status")
public class IdamConfigStatusEndpoint {

    private final AuthenticationChecker authenticationChecker;

    public IdamConfigStatusEndpoint(AuthenticationChecker authenticationChecker) {
        this.authenticationChecker = authenticationChecker;
    }

    @ReadOperation
    public List<JurisdictionConfigurationStatus> jurisdictions() {
        return authenticationChecker.checkSignInForAllJurisdictions();
    }

    @ReadOperation
    public JurisdictionConfigurationStatus jurisdiction(@Selector String jurisdiction) {
        return authenticationChecker.checkSignInForJurisdiction(jurisdiction);
    }
}
