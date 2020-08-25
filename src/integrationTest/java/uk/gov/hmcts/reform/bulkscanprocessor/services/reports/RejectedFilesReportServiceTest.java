package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RejectedFilesReportServiceTest {

    @Autowired
    private BlobManagementProperties blobManagementProperties;

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
    public void setUp() {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString("UseDevelopmentStorage=true")
            .buildClient();

        this.blobManager = new BlobManager(blobServiceClient, blobManagementProperties);

        this.rejectedContainer = blobServiceClient.getBlobContainerClient("test-rejected");
        if (!this.rejectedContainer.exists()) {
            this.rejectedContainer.create();
        }
    }

    @Test
    public void should_read_files_from_rejected_container() {
        // given
        // there are two files in rejected container
        rejectedContainer.getBlobClient("foo.zip")
            .upload(new ByteArrayInputStream("some content".getBytes()),"some content".getBytes().length);
        rejectedContainer.getBlobClient("bar.zip")
            .upload(new ByteArrayInputStream("some content".getBytes()),"some content".getBytes().length);

        assertThat(rejectedContainer.listBlobs()).hasSize(2); // sanity check

        // when
        List<RejectedFile> result = new RejectedFilesReportService(blobManager).getRejectedFiles();

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new RejectedFile("foo.zip", "test-rejected"),
                new RejectedFile("bar.zip", "test-rejected")
            );
    }
}
