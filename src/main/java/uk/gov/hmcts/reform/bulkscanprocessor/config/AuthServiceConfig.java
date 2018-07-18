package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
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
@EnableFeignClients(basePackageClasses = ServiceAuthorisationApi.class)
public class AuthServiceConfig {

    @Bean
    public AuthTokenGenerator authTokenGenerator(
        @Value("${idam.s2s-auth.secret}") String secret,
        @Value("${idam.s2s-auth.name}") String name,
        ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, name, serviceAuthorisationApi);
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
            public void validate(String token) {
                throw new NotImplementedException();
            }

            public void validate(String token, List<String> roles) {
                throw new NotImplementedException();
            }

            public String getServiceName(String token) {
                return "some_service_name";
            }
        };
    }
}
