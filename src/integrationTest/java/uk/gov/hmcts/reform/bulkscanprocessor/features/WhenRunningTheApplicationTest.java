package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "scheduling.enabled=true",
        "scan.delay=1"
    }
)
@RunWith(SpringRunner.class)
public class WhenRunningTheApplicationTest {

    private static final int SCHEDULE_MULTIPLIER = 4;

    @Value("${scan.delay}")
    private int scanDelay;

    @SpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() {
        // given
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // when
        waitForBlobProcessor();

        // then
        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getValue().getName()).isEqualTo("blobProcessor");
    }

    private void waitForBlobProcessor() {
        try {
            Thread.sleep(scanDelay * 2 * SCHEDULE_MULTIPLIER);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean
        public CloudBlobClient getCloudBlobClient(@Value("${scan.delay}") int scanDelay) {
            CloudBlobClient client = mock(CloudBlobClient.class);

            when(client.listContainers()).thenAnswer(invocation -> {
                Thread.sleep(scanDelay * 2);

                return Collections.emptyList();
            });

            return client;
        }
    }
}
