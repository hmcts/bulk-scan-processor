package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker.LEASE_EXPIRATION_TIME;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

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

        this.blobManager = new BlobManager(blobServiceClient, null, blobManagementProperties);

        this.rejectedContainer = blobServiceClient.getBlobContainerClient(("test-rejected"));
        if (!this.rejectedContainer.exists()) {
            this.rejectedContainer.create();
        }
    }

    @AfterEach
    protected void deleteAllContainers() {
        this.rejectedContainer.delete();
    }

    @Test
    public void should_delete_old_files_with_snapshots() throws Exception {
        // given
        // there are two files in rejected container
        uploadFile(rejectedContainer, "foo.zip", "content_foo_1");
        uploadFile(rejectedContainer, "bar.zip", "content_bar_2");

        assertThat(rejectedContainer.listBlobs()).hasSize(2); // sanity check

        Map<String, String> blobMetaData = new HashMap<>();
        // add expired leaseExpirationTime to metadata
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).minusSeconds(10).toString());
        // one of them has a snapshot and expired lease metadata
        rejectedContainer
            .getBlobClient("bar.zip")
            .createSnapshot()
            .setMetadata(blobMetaData);


        // when
        new CleanUpRejectedFilesTask(blobManager, leaseAcquirer, "PT0H").run();

        // then
        assertThat(rejectedContainer.listBlobs()).isEmpty();
    }

    @Test
    public void should_not_delete_old_file_if_metadata_lease_not_expired() throws Exception {
        // given
        // there are two files in rejected container
        uploadFile(rejectedContainer, "foo.zip", "content_foo");
        uploadFile(rejectedContainer, "bar.zip", "content_bar");

        assertThat(rejectedContainer.listBlobs()).hasSize(2); // sanity check

        Map<String, String> blobMetaData = new HashMap<>();
        // add non-expired leaseExpirationTime to metadata
        blobMetaData.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusSeconds(90).toString());

        // one of them has vslid lease metadata
        rejectedContainer
            .getBlobClient("bar.zip")
            .setMetadata(blobMetaData);

        // when
        new CleanUpRejectedFilesTask(blobManager, leaseAcquirer, "PT0H").run();

        // then
        assertThat(rejectedContainer.listBlobs()).isNotEmpty();
        List<BlobItem> listBlobItem = rejectedContainer.listBlobs().stream().collect(Collectors.toList());
        assertThat(listBlobItem.size()).isEqualTo(1);
        assertThat(listBlobItem.get(0).getName()).isEqualTo("bar.zip");

    }

    private void uploadFile(BlobContainerClient containerClient, String fileName, String content) {
        byte[] byteContent = content.getBytes();
        containerClient.getBlobClient(fileName)
            .upload(new ByteArrayInputStream(byteContent), byteContent.length);
    }
}
