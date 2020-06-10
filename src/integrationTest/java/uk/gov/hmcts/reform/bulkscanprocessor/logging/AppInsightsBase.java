package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.mockito.Mockito.mock;

@IntegrationTest
@RunWith(SpringRunner.class)
public class AppInsightsBase {

    @MockBean
    protected TelemetryClient telemetry;

    @Captor
    protected ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor;

    @TestConfiguration
    public static class MockConfig {

        @Bean(name = "notifications-helper")
        public ServiceBusHelper notificationsQueueHelper() {
            return mock(ServiceBusHelper.class);
        }

        @Bean(name = "envelopes-helper")
        public ServiceBusHelper envelopesQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
