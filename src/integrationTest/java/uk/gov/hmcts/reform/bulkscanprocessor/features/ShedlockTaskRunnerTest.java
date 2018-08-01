package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;

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
public class ShedlockTaskRunnerTest {

    private static final int SCHEDULE_MULTIPLIER = 4;

    @Value("${scan.delay}")
    private int scanDelay;

    @SpyBean
    private LockProvider lockProvider;

    @Autowired
    private DataSource dataSource;

    @Test
    public void should_integrate_with_shedlock() throws SQLException {
        // given
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // when
        waitForBlobProcessor();

        // then
        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getValue().getName()).isEqualTo("blobProcessor");

        // and
        List<String> locks = new ArrayList<>();
        Connection connection = dataSource.getConnection();
        ResultSet resultSet = connection.prepareStatement("SELECT * FROM shedlock").executeQuery();

        while (resultSet.next()) {
            locks.add(resultSet.getString("name"));
        }

        connection.close();

        assertThat(locks)
            .hasSize(1)
            .contains("blobProcessor");
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
