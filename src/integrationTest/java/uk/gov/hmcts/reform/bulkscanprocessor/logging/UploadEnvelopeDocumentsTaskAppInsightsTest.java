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

@TestPropertySource(
    properties = {
        "scheduling.task.upload-documents.delay=1000",
        "scheduling.task.upload-documents.enabled=true",
        "scheduling.task.upload-documents.max_tries=3"
    }
)
public class UploadEnvelopeDocumentsTaskAppInsightsTest extends AppInsightsBase {

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @Test
    public void should_trace_when_success() throws InterruptedException {

        given(envelopeRepository.findEnvelopesToUpload(3)).willReturn(Arrays.asList());

        TimeUnit.SECONDS.sleep(5);

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /UploadEnvelopeDocumentsTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(envelopeRepository.findEnvelopesToUpload(3)).willThrow(new RuntimeException("failed"));

        TimeUnit.SECONDS.sleep(5);
        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /UploadEnvelopeDocumentsTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }
}
