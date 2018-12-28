package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.FailedDocUploadProcessor;

import java.io.File;
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

    private static DockerComposeContainer dockerComposeContainer;

    @BeforeClass
    public static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @AfterClass
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

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
            + FailedDocUploadProcessor.class.getCanonicalName()
            + ":\\d+: An error occurred when processing failed documents for jurisdiction SSCS\\."
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
