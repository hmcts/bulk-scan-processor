package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class CleanUpRejectedFilesTaskTest {

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    @Autowired
    private LeaseAcquirer leaseAcquirer;

    @Autowired
    private BlobServiceClient blobServiceClient;

    private BlobContainerClient rejectedContainer;

    private BlobManager blobManager;

    private static DockerComposeContainer dockerComposeContainer;

    @BeforeAll
    public static void initialize() {
        dockerComposeContainer =
            new DockerComposeContainer(new File("src/integrationTest/resources/docker-compose.yml"))
                .withExposedService("azure-storage", 10000);

        dockerComposeContainer.start();
    }

    @AfterAll
    public static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {

        this.blobManager = new BlobManager(blobServiceClient, null, blobManagementProperties);

        this.rejectedContainer = blobServiceClient.getBlobContainerClient(("test-rejected"));
        if (!this.rejectedContainer.exists()) {
            this.rejectedContainer.create();
        }
    }

    @Test
    public void should_delete_old_files() throws Exception {
        // given
        // there are two files in rejected container
        rejectedContainer.getBlobClient("foo.zip")
            .upload(new ByteArrayInputStream("some content".getBytes()),"some content".getBytes().length);
        rejectedContainer.getBlobClient("bar.zip")
            .upload(new ByteArrayInputStream("some content".getBytes()),"some content".getBytes().length);


        assertThat(rejectedContainer.listBlobs()).hasSize(2); // sanity check

        // one of them has a snapshot
        rejectedContainer
            .getBlobClient("bar.zip")
            .createSnapshot();

        // when
        new CleanUpRejectedFilesTask(blobManager, leaseAcquirer, "PT0H").run();

        // then
        assertThat(rejectedContainer.listBlobs()).isEmpty();
    }
}
