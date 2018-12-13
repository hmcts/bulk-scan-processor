package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    properties = {
        "scheduling.task.scan.enabled=true",
        "scheduling.task.reupload.enabled=true"
    }
)
@RunWith(SpringRunner.class)
public class WhenRunningTheApplicationTest {

    @SpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() {
        waitForBlobProcessor();
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);
        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getAllValues()).extracting("name").containsOnly("re-upload-failures");
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
        public CloudBlobClient getCloudBlobClient() throws InvalidKeyException, URISyntaxException {
            return CloudStorageAccount
                .parse("UseDevelopmentStorage=true")
                .createCloudBlobClient();
        }

        @Bean(name = "notifications")
        public ServiceBusHelper serviceBusHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
