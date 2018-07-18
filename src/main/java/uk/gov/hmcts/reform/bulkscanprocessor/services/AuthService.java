package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;

@Component
public class AuthService {

    private final AuthTokenValidator authTokenValidator;

    public AuthService(AuthTokenValidator authTokenValidator) {
        this.authTokenValidator = authTokenValidator;
    }

    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnAuthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }
}
