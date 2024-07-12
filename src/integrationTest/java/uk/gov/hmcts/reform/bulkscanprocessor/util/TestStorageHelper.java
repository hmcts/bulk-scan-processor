package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.testcontainers.containers.GenericContainer;

import java.io.ByteArrayInputStream;

import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

public class TestStorageHelper {

    private static final String AZURE_TEST_CONTAINER = "hmctspublic.azurecr.io/imported/azure-storage/azurite:3.29.0";
    private static final String EXTRACTION_HOST = "azurite";
    private static final int CONTAINER_PORT = 10000;

    private static TestStorageHelper INSTANCE;

    public static final String CONTAINER_NAME = "bulkscan";
    public static final String ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    private static String DOCKER_HOST;
    public static final String STORAGE_CONN_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
        + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
        + "BlobEndpoint=http://%s:%d/devstoreaccount1;";
    public static BlobServiceClient BLOB_SERVICE_CLIENT;
    private BlobContainerClient testContainer;

    private static GenericContainer<?> DOCKER_COMPOSE_CONTAINER =
        new GenericContainer<>(AZURE_TEST_CONTAINER).withExposedPorts(CONTAINER_PORT);

    private TestStorageHelper() {
        // empty constructor
    }

    public static TestStorageHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TestStorageHelper();
        }

        return INSTANCE;
    }

    public static void initialize() {
        DOCKER_COMPOSE_CONTAINER.withEnv("executable", "blob");
        DOCKER_COMPOSE_CONTAINER.withNetworkAliases(EXTRACTION_HOST);
        DOCKER_COMPOSE_CONTAINER.start();
        DOCKER_HOST = DOCKER_COMPOSE_CONTAINER.getHost();

        BLOB_SERVICE_CLIENT = new BlobServiceClientBuilder()
            .connectionString(
                String.format(STORAGE_CONN_STRING,
                              DOCKER_HOST,
                              DOCKER_COMPOSE_CONTAINER.getMappedPort(CONTAINER_PORT))
            )
            .buildClient();
    }

    public static void stopDocker() {
        DOCKER_COMPOSE_CONTAINER.stop();
    }

    public void createBulkscanContainer() {
        testContainer = BLOB_SERVICE_CLIENT.getBlobContainerClient(CONTAINER_NAME);
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
