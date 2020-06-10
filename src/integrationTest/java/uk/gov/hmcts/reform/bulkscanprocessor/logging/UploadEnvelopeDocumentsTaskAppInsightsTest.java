package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.UploadEnvelopeDocumentsTask;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@TestPropertySource(
    properties = {
        "scheduling.task.upload-documents.delay=99999",
        "scheduling.task.upload-documents.enabled=true",
        "scheduling.task.upload-documents.max_tries=3"
    }
)
@SpringBootTest
@RunWith(SpringRunner.class)
public class UploadEnvelopeDocumentsTaskAppInsightsTest {

    @SpyBean
    protected TelemetryClient telemetry;

    @Captor
    protected ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor;

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private UploadEnvelopeDocumentsTask uploadEnvelopeDocumentsTask;

    @Test
    public void should_trace_when_success() throws InterruptedException {

        given(envelopeRepository.findEnvelopesToUpload(3)).willReturn(Arrays.asList());

        uploadEnvelopeDocumentsTask.run();

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /UploadEnvelopeDocumentsTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(envelopeRepository.findEnvelopesToUpload(3)).willThrow(new RuntimeException("failed"));

        try {
            uploadEnvelopeDocumentsTask.run();
        } catch (Exception ex) {
            //ignore
        }

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /UploadEnvelopeDocumentsTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }
}
