package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RejectedEnvelopesReportServiceTest {

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
    public void should_read_files_from_rejected_container() throws Exception {
        // given
        // there are two files in rejected container
        rejectedContainer.getBlockBlobReference("foo.zip").uploadText("some content");
        rejectedContainer.getBlockBlobReference("bar.zip").uploadText("some content");

        assertThat(rejectedContainer.listBlobs()).hasSize(2); // sanity check

        // when
        List<RejectedEnvelope> result = new RejectedEnvelopesReportService(blobManager).getRejectedEnvelopes();

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new RejectedEnvelope("foo.zip", "test-rejected"),
                new RejectedEnvelope("bar.zip", "test-rejected")
            );
    }
}
