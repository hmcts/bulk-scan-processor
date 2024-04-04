package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseClientProvider;

import static org.mockito.Mockito.mock;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;
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

    @Bean(name = "processed-envelopes-client")
    @Profile(SERVICE_BUS_STUB)
    public ServiceBusProcessorClient processedEnvelopesQueueProcessor() {
        return mock(ServiceBusProcessorClient.class);
    }

    @Bean
    @Profile(STORAGE_STUB)
    public BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString("UseDevelopmentStorage=true")
            .buildClient();
    }

    @Bean
    @Profile(STORAGE_STUB)
    public LeaseClientProvider getLeaseClientProvider() {
        return blobClient -> new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
    }

    @Bean(name = "envelopes-helper")
    public ServiceBusSendHelper envelopesQueueHelper() {
        return mock(ServiceBusSendHelper.class);
    }
}
