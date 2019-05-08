package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.mockito.Mockito.mock;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles.SERVICE_BUS_STUB;
import static uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles.STORAGE_STUB;

@Configuration
public class IntegrationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String PROFILE_WIREMOCK = "wiremock";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        boolean wireMockIsEnabled = applicationContext.getEnvironment().acceptsProfiles(Profiles.of(PROFILE_WIREMOCK));

        if (wireMockIsEnabled) {
            System.setProperty("wiremock.port", Integer.toString(findAvailableTcpPort()));
        }
    }

    @Bean
    @Profile(PROFILE_WIREMOCK)
    public Options options(@Value("${wiremock.port}") int port) {
        return WireMockConfiguration.options().port(port).notifier(new Slf4jNotifier(false));
    }

    @Bean(name = "processed-envelopes-completor")
    @Profile(SERVICE_BUS_STUB)
    public MessageAutoCompletor processedEnvelopesCompletor() {
        return mock(MessageAutoCompletor.class);
    }

    @Bean
    @Profile(STORAGE_STUB)
    public CloudBlobClient getCloudBlobClient() throws InvalidKeyException, URISyntaxException {
        return CloudStorageAccount
            .parse("UseDevelopmentStorage=true")
            .createCloudBlobClient();
    }
}
