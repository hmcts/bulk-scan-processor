package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "scheduling.task.reupload.enabled=true"
})
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

    @After
    public void tearDown() {
        outputCapture.flush();
    }

    @Test
    public void should_return_new_instance_of_processor_when_rerequesting_via_lookup_method() {
        // comparing instance hashes. .hashCode() just returns same one
        assertThat(task.getProcessor().toString()).isNotEqualTo(task.getProcessor().toString());
    }
}
