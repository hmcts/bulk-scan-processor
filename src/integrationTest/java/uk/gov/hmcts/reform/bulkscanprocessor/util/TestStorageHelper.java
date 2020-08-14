package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.ByteArrayInputStream;
import java.io.File;

import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

public class TestStorageHelper {

    private static TestStorageHelper INSTANCE;

    public static final String CONTAINER_NAME = "bulkscan";
    public static final String ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    private static DockerComposeContainer<?> dockerContainer;
    public static BlobServiceClient blobServiceClient;
    private BlobContainerClient testContainer;

    private TestStorageHelper() {
        // empty constructor
    }

    public static TestStorageHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestStorageHelper();
        }

        return INSTANCE;
    }

    private static void createDocker() {
        dockerContainer = new DockerComposeContainer<>(
            new File("src/integrationTest/resources/docker-compose.yml")
        ).withExposedService("azure-storage", 10000);
        dockerContainer.start();
    }

    private static void initializeStorage() {
        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString("UseDevelopmentStorage=true")
            .buildClient();

    }

    public static void initialize() {
        createDocker();
        initializeStorage();
    }

    public static void stopDocker() {
        dockerContainer.stop();
    }

    public void createBulkscanContainer() {
        testContainer = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        testContainer.create();
    }

    public void deleteBulkscanContainer() {
        testContainer.delete();
    }

    public void upload(String directory) {
        try {
            BlobClient blobClient = testContainer.getBlobClient(ZIP_FILE_NAME);

            // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
            // due to doc upload failure
            if (blobClient.exists()) {
                blobClient.delete();
            }

            byte[] fileContent = zipDir(directory);
            blobClient.upload(new ByteArrayInputStream(fileContent), fileContent.length, true);
        } catch (Exception exception) {
            throw new RuntimeException("Unable to upload zip to test container", exception);
        }
    }

    /**
     * Uploads the default contents which are under 'zipcontents/ok' directory.
     */
    public void upload() {
        upload("zipcontents/ok");
    }
}
