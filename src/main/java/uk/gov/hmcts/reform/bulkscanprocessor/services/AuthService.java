package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;

/**
 * Service to authenticate service-to-service requests.
 */
@Component
public class AuthService {

    private final AuthTokenValidator authTokenValidator;

    /**
     * Constructor for AuthService.
     * @param authTokenValidator AuthTokenValidator
     */
    public AuthService(AuthTokenValidator authTokenValidator) {
        this.authTokenValidator = authTokenValidator;
    }

    /**
     * Authenticates the service-to-service request.
     * @param authHeader ServiceAuthorization header
     * @return Service name
     */
    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnAuthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }
}
