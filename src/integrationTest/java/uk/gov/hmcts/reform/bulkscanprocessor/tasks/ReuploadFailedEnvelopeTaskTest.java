package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.palantir.docker.compose.DockerComposeRule;
import com.palantir.docker.compose.connection.waiting.HealthChecks;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
    "scheduling.task.reupload.enabled=true"
})
@RunWith(SpringRunner.class)
public class ReuploadFailedEnvelopeTaskTest {

    @ClassRule
    public static DockerComposeRule docker = DockerComposeRule.builder()
        .file("src/integrationTest/resources/docker-compose.yml")
        .waitingForService("azure-storage", HealthChecks.toHaveAllPortsOpen())
        .waitingForService("azure-storage", HealthChecks.toRespondOverHttp(10000, (port) -> port.inFormat("http://$HOST:$EXTERNAL_PORT/devstoreaccount1?comp=list")))
        .build();

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Autowired
    private ReuploadFailedEnvelopeTask task;

    @SpyBean
    private EnvelopeProcessor envelopeProcessor;

    @After
    public void tearDown() {
        outputCapture.flush();
    }

    @Test
    public void should_return_new_instance_of_processor_when_rerequesting_via_lookup_method() {
        // comparing instance hashes. .hashCode() just returns same one
        assertThat(task.getProcessor().toString()).isNotEqualTo(task.getProcessor().toString());
    }

    @Test
    public void should_await_for_all_futures_even_if_their_code_throw_exception() throws Exception {
        // given
        given(envelopeProcessor.getFailedToUploadEnvelopes(anyString()))
            .willThrow(JpaObjectRetrievalFailureException.class);

        // when
        task.processUploadFailures();

        // then
        assertThat(outputCapture.toString()).containsPattern(".+ERROR \\[.+\\] "
            + ReuploadFailedEnvelopeTask.class.getCanonicalName()
            + ":\\d+: "
            + JpaObjectRetrievalFailureException.class.getCanonicalName()
        );
    }

    @TestConfiguration
    static class StorageTestConfiguration {

        @Bean
        public CloudBlobClient getCloudBlobClient() throws InvalidKeyException, URISyntaxException {
            CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");

            return account.createCloudBlobClient();
        }
    }
}
