package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.logging.OrchestratorNotificationTaskAppInsightsTest.MockConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.OrchestratorNotificationTask;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@TestPropertySource(
    properties = {
        "scheduling.task.notifications_to_orchestrator.enabled=true"
    }
)
@ContextConfiguration(classes = {OrchestratorNotificationTask.class, AppInsights.class, MockConfig.class})
@RunWith(SpringRunner.class)
public class OrchestratorNotificationTaskAppInsightsTest {

    @SpyBean
    protected TelemetryClient telemetry;

    @Captor
    protected ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor;

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private OrchestratorNotificationTask orchestratorNotificationTask;

    @Test
    public void should_trace_when_success() throws InterruptedException {

        given(envelopeRepository.findByStatus(UPLOADED)).willReturn(Arrays.asList());
        doNothing().when(telemetry).trackRequest(any());

        orchestratorNotificationTask.run();

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /OrchestratorNotificationTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(envelopeRepository.findByStatus(UPLOADED)).willThrow(new RuntimeException("failed"));
        doNothing().when(telemetry).trackRequest(any());

        try {
            orchestratorNotificationTask.run();
        } catch (Exception ex) {
            //ignore
        }

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /OrchestratorNotificationTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Configuration
    public static class MockConfig {
        @Bean(name = "envelopes-helper")
        public ServiceBusHelper envelopesQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
        @Bean
        public EnvelopeRepository envelopeRepo() {
            return mock(EnvelopeRepository.class);
        }
        @Bean
        public ProcessEventRepository processEventRepo() { return mock(ProcessEventRepository.class); }
        @Bean
        public TelemetryClient telemetryClient() { return mock(TelemetryClient.class); }
    }
}
