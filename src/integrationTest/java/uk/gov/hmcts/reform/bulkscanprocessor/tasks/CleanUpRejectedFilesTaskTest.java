package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.File;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CleanUpRejectedFilesTaskTest {

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    private CloudBlobContainer rejectedContainer;
    private BlobManager blobManager;

    private static DockerComposeContainer dockerComposeContainer;

    @BeforeClass
    public static void initialize() {
        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @AfterClass
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @Before
    public void setUp() throws Exception {
        CloudStorageAccount account = CloudStorageAccount.parse("UseDevelopmentStorage=true");
        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();

        this.blobManager = new BlobManager(cloudBlobClient, blobManagementProperties);

        this.rejectedContainer = cloudBlobClient.getContainerReference("test-rejected");
        this.rejectedContainer.createIfNotExists();
    }

    @Test
    public void should_delete_old_files() throws Exception {
        // given
        upload("foo.zip");
        upload("bar.zip");

        // sanity check
        assertThat(rejectedContainer.listBlobs()).hasSize(2);

        // add snapshot to one of the files
        rejectedContainer
            .getBlockBlobReference("bar.zip")
            .createSnapshot();

        // when
        new CleanUpRejectedFilesTask(blobManager, Duration.ZERO).run();

        // then
        assertThat(rejectedContainer.listBlobs()).isEmpty();
    }

    private void upload(String fileName) throws Exception {
        rejectedContainer
            .getBlockBlobReference(fileName)
            .uploadText("some content");
    }
}
