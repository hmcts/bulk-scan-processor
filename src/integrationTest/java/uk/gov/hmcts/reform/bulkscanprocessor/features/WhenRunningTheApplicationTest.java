package uk.gov.hmcts.reform.bulkscanprocessor.features;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
    properties = {
        "scheduling.enabled=true"
    }
)
@RunWith(SpringRunner.class)
public class WhenRunningTheApplicationTest {

    @Autowired
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() {
        waitForBlobProcessor();
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getValue().getName()).isEqualTo("blobProcessor");
    }

    private void waitForBlobProcessor() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean
        public LockProvider lockProvider() {
            LockProvider mock = Mockito.mock(LockProvider.class);
            when(mock.lock(any())).thenReturn(Optional.empty());
            return mock;
        }
    }
}
