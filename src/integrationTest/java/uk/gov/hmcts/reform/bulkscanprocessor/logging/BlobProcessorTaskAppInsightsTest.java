package uk.gov.hmcts.reform.bulkscanprocessor.logging;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.BlobProcessorTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@TestPropertySource(
    properties = {
        "scheduling.task.scan.enabled=true",
        "scheduling.task.scan.delay=94000"
    }
)
@IntegrationTest
@RunWith(SpringRunner.class)
public class BlobProcessorTaskAppInsightsTest {

    @MockBean
    protected TelemetryClient telemetry;

    @Captor
    protected ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor;

    @MockBean
    private BlobManager blobManager;

    @Autowired
    private BlobProcessorTask blobProcessorTask;

    @Test
    public void should_trace_when_success() {
        given(blobManager.listInputContainers()).willReturn(Arrays.asList());

        blobProcessorTask.processBlobs();

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /BlobProcessorTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() {
        given(blobManager.listInputContainers()).willThrow(new RuntimeException("failed"));

        try {
            blobProcessorTask.processBlobs();
        } catch (Exception ex) {
            //ignore
        }

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /BlobProcessorTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @TestConfiguration
    public static class MockConfig {
        @Bean(name = "notifications-helper")
        public ServiceBusHelper notificationsQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
