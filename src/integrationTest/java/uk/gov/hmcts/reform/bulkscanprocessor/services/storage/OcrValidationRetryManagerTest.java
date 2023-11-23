package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import com.azure.storage.blob.specialized.BlobLeaseClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.DockerComposeContainer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestStorageHelper;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

@IntegrationTest
class OcrValidationRetryManagerTest {
    private static final String CONTAINER_NAME = "bulkscan";

    private static final String SAMPLE_ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    @Autowired
    private OcrValidationRetryManager ocrValidationRetryManager;

    private static DockerComposeContainer dockerComposeContainer;
    private static String dockerHost;

    private BlobContainerClient testContainer;

    @BeforeEach
    void setUp() {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(String.format(TestStorageHelper.STORAGE_CONN_STRING, dockerHost, 10000))
            .buildClient();

        testContainer = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        if (!testContainer.exists()) {
            testContainer.create();
        }
    }

    @AfterEach
    void cleanUp() {
        if (testContainer.exists()) {
            testContainer.delete();
        }
    }

    @BeforeAll
    static void initialize() {
        File dockerComposeFile = new File("src/integrationTest/resources/docker-compose.yml");

        dockerComposeContainer = new DockerComposeContainer(dockerComposeFile)
            .withExposedService("azure-storage", 10000)
            .withLocalCompose(true);

        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getServiceHost("azure-storage", 10000);
    }

    @AfterAll
    static void tearDownContainer() {
        dockerComposeContainer.stop();
    }

    @Test
    void should_read_blob_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        BlobClient blobClient = testContainer.getBlobClient(SAMPLE_ZIP_FILE_NAME);

        BlobLeaseClient leaseClient = new BlobLeaseClientBuilder().blobClient(blobClient).buildClient();
        var leaseId = leaseClient.acquireLease(60);

        // when
        // then
        // retry is possible if ocrValidationRetryDelayExpirationTime metadata property is not set
        assertThat(ocrValidationRetryManager.canProcess(blobClient)).isTrue();

        assertThat(ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, leaseId)).isTrue();

        doSleep(1000L);
        // retry is not possible if retry delay (2 sec) has not expired
        assertThat(ocrValidationRetryManager.canProcess(blobClient)).isFalse();

        doSleep(5000L);
        // retry is possible if retry delay (2 sec) has expired
        assertThat(ocrValidationRetryManager.canProcess(blobClient)).isTrue();

        // and ocrValidationRetryCount metadata property is not exceeding 1
        assertThat(ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, leaseId)).isTrue();

        doSleep(1000L);
        // retry is not possible if retry delay (3 sec) has not expired
        assertThat(ocrValidationRetryManager.canProcess(blobClient)).isFalse();

        doSleep(5000L);
        // retry is possible if retry delay (3 sec) has expired
        assertThat(ocrValidationRetryManager.canProcess(blobClient)).isTrue();

        // but ocrValidationRetryCount metadata property is now exceeding 1 and result is false
        assertThat(ocrValidationRetryManager.setRetryDelayIfPossible(blobClient, leaseId)).isFalse();

        leaseClient.releaseLease();
    }

    private void doSleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void uploadToBlobStorage(String fileName, byte[] fileContent) {
        var blockBlobReference = testContainer.getBlobClient(fileName);

        // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
        // due to doc upload failure
        if (blockBlobReference.exists()) {
            blockBlobReference.delete();
        }

        // A Put Blob operation may succeed against a blob that exists in the storage emulator with an active lease,
        // even if the lease ID has not been specified in the request.
        blockBlobReference.upload(new ByteArrayInputStream(fileContent), fileContent.length);
    }
}
