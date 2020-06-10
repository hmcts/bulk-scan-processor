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
import uk.gov.hmcts.reform.bulkscanprocessor.logging.DeleteCompleteFilesTaskAppInsightsTest_backup.MockConfig;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.DeleteCompleteFilesTask;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@TestPropertySource(
    properties = {
        "scheduling.task.delete-complete-files.enabled=true"
    }
)
@ContextConfiguration(classes = {DeleteCompleteFilesTask.class, AppInsights.class, MockConfig.class})
@RunWith(SpringRunner.class)
public class DeleteCompleteFilesTaskAppInsightsTest {

    @SpyBean
    protected TelemetryClient telemetry;

    @Captor
    protected ArgumentCaptor<RequestTelemetry> telemetryRequestCaptor;

    @MockBean
    private BlobManager blobManager;

    @Autowired
    private DeleteCompleteFilesTask deleteCompleteFilesTask;

    @Test
    public void should_trace_when_success() throws InterruptedException {
        given(blobManager.listInputContainers()).willReturn(Arrays.asList());
        doNothing().when(telemetry).trackRequest(any());

        deleteCompleteFilesTask.run();

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /DeleteCompleteFilesTask");
        assertThat(requestTelemetry.isSuccess()).isTrue();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }

    @Test
    public void should_trace_when_failed() throws InterruptedException {
        given(blobManager.listInputContainers()).willThrow(new RuntimeException("failed"));
        doNothing().when(telemetry).trackRequest(any());

        try {
            deleteCompleteFilesTask.run();
        } catch (Exception ex) {
            //ignore
        }

        verify(telemetry, atLeastOnce()).trackRequest(telemetryRequestCaptor.capture());

        RequestTelemetry requestTelemetry = telemetryRequestCaptor.getValue();

        assertThat(requestTelemetry.getName()).isEqualTo("Schedule /DeleteCompleteFilesTask");
        assertThat(requestTelemetry.isSuccess()).isFalse();
        assertThat(requestTelemetry.getId()).isNotBlank();
    }


    @Configuration
    public static class MockConfig {

        @Bean
        public BlobManager blobManager() {
            return mock(BlobManager.class);
        }

        @Bean
        public EnvelopeRepository envelopeRepository() {
            return mock(EnvelopeRepository.class);
        }

        @Bean
        public TelemetryClient telemetryClient() {
            return mock(TelemetryClient.class);
        }

    }
}
