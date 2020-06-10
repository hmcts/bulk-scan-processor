package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@TestPropertySource(
    properties = {
        "scheduling.task.notifications_to_orchestrator.delay=500",
        "scheduling.task.notifications_to_orchestrator.enabled=true"
    }
)
public class OrchestratorNotificationTaskAppInsightsTest extends AppInsightsBase {

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @Test
    public void should_trace_when_success() throws InterruptedException {

        given(envelopeRepository.findByStatus(UPLOADED)).willReturn(Arrays.asList());

        TimeUnit.SECONDS.sleep(5);

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /OrchestratorNotificationTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(envelopeRepository.findByStatus(UPLOADED)).willThrow(new RuntimeException("failed"));

        TimeUnit.SECONDS.sleep(5);
        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /OrchestratorNotificationTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }
}
