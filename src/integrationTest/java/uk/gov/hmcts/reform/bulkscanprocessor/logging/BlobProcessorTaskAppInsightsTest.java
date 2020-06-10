package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@TestPropertySource(
    properties = {
        "scheduling.task.scan.enabled=true",
        "scheduling.task.scan.delay=400"
    }
)
public class BlobProcessorTaskAppInsightsTest extends AppInsightsBase {

    @MockBean
    private BlobManager blobManager;

    @Test
    public void should_trace_when_success() throws InterruptedException {
        given(blobManager.listInputContainers()).willReturn(Arrays.asList());

        TimeUnit.MILLISECONDS.sleep(600);

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /BlobProcessorTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(blobManager.listInputContainers()).willThrow(new RuntimeException("failed"));

        TimeUnit.MILLISECONDS.sleep(600);

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /BlobProcessorTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }
}
