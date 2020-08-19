package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RejectedFilesReportServiceTest {

    @Autowired
    private BlobManagementProperties blobManagementProperties;

    private BlobContainerClient rejectedContainer;
    private BlobManager blobManager;

    private static GenericContainer<?> dockerContainer;

    @BeforeAll
    public static void initialize() {
        dockerContainer = new FixedHostPortGenericContainer<>(new DockerImageName("arafato/azurite", "2.6.5").toString())
            .withEnv(ImmutableMap.of("executable", "blob"))
            .withFixedExposedPort(10000, 10000);

        dockerContainer.start();
    }

    @AfterAll
    public static void tearDownContainer() {
        dockerContainer.stop();
    }

    @BeforeEach
    public void setUp() throws Exception {

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString("UseDevelopmentStorage=true")
            .buildClient();

        this.blobManager = new BlobManager(blobServiceClient, null, blobManagementProperties);

        this.rejectedContainer = blobServiceClient.getBlobContainerClient("test-rejected");
        if (!this.rejectedContainer.exists()) {
            this.rejectedContainer.create();
        }
    }

    @Test
    public void should_read_files_from_rejected_container() throws Exception {
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
