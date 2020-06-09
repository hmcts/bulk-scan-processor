package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@IntegrationTest
@RunWith(SpringRunner.class)
@TestPropertySource(
    properties = {
        "scheduling.task.scan.enabled=true",
        "scheduling.task.upload-documents.delay=1000",
        "scheduling.task.upload-documents.enabled=true",
        "scheduling.task.notifications_to_orchestrator.enabled=true",
        "scheduling.task.delete-complete-files.enabled=true",
        "scheduling.task.delete-complete-files.cron=* * * * * *"
    }
)
public class SchedulerConfigTest {

    @SpyBean
    private LockProvider lockProvider;

    @SpyBean
    private TelemetryClient telemetry;

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);
        doNothing().when(telemetry).trackRequest(any());

        // wait for asynchronous run of the scheduled task in background
        Thread.sleep(6000);

        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor = ArgumentCaptor.forClass(RequestTelemetry.class);
        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        assertThat(configCaptor.getAllValues())
            .extracting(lc -> lc.getName())
            .containsOnly(
                "upload-documents",
                "send-orchestrator-notification",
                "delete-complete-files"
            );

        assertThat(telemetryRequestCaptor.getAllValues())
            .extracting(lc -> lc.getName())
            .containsAnyOf(
                "Schedule /UploadEnvelopeDocumentsTask",
                "Schedule /OrchestratorNotificationTask",
                "Schedule /DeleteCompleteFilesTask"
            );
    }

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

        @Bean
        public TelemetryClient telemetryClient() {
            return mock(TelemetryClient.class);
        }

    }
}
