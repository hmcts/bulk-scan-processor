package uk.gov.hmcts.reform.bulkscanprocessor.features;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@IntegrationTest
@TestPropertySource(
    properties = {
        "scheduling.task.scan.enabled=true",
        "scheduling.task.upload-documents.delay=1000",
        "scheduling.task.upload-documents.enabled=true",
        "scheduling.task.notifications_to_orchestrator.enabled=true",
        "scheduling.task.delete-complete-files.enabled=true",
        "scheduling.task.delete-complete-files.cron=* * * * * *",
        "monitoring.incomplete-envelopes.enabled=true",
        "monitoring.incomplete-envelopes.cron=* * * * * *"
    }
)
public class SchedulerConfigTest {

    @SpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // wait for asynchronous run of the scheduled task in background
        Thread.sleep(2000);

        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getAllValues())
            .extracting(lc -> lc.getName())
            .containsOnly(
                "upload-documents",
                "send-orchestrator-notification",
                "delete-complete-files",
                "incomplete-envelopes-monitoring"
            );
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean(name = "notifications-helper")
        public ServiceBusHelper notificationsQueueHelper() {
            return mock(ServiceBusHelper.class);
        }

    }
}
