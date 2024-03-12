package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.List;

@Configuration
@Lazy
public class AuthServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url")
    public AuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.secret}") String secret,
        @Value("${idam.s2s-auth.name}") String name,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, name, serviceAuthorisationApi);
    }

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenGenerator authTokenGeneratorStub() {
        return () -> {
            throw new NotImplementedException();
        };
    }

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url")
    public AuthTokenValidator tokenValidator(ServiceAuthorisationApi s2sApi) {
        return new ServiceAuthTokenValidator(s2sApi);
    }

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenValidator tokenValidatorStub() {
        return new AuthTokenValidator() {
            @Override
            public void validate(String token) {
                throw new NotImplementedException();
            }

            @Override
            public void validate(String token, List<String> roles) {
                throw new NotImplementedException();
            }

            @Override
            public String getServiceName(String token) {
                return "some_service_name";
            }
        };
    }
}
