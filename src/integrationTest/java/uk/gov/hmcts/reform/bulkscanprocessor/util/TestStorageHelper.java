package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

public class TestStorageHelper {

    private static TestStorageHelper INSTANCE;

    public static final String CONTAINER_NAME = "bulkscan";
    public static final String ZIP_FILE_NAME = "1_24-06-2018-00-00-00.zip";

    private static DockerComposeContainer<?> dockerComposeContainer;
    private static String dockerHost;
    private static final String STORAGE_CONN_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
        + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
        + "BlobEndpoint=http://%s:%d/devstoreaccount1;";
    public static CloudBlobClient cloudBlobClient;
    private CloudBlobContainer testContainer;

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
        dockerComposeContainer = new DockerComposeContainer<>(
            new File("src/integrationTest/resources/docker-compose.yml")
        ).withExposedService("azure-storage", 10000)
        .withLocalCompose(true);
        dockerComposeContainer.start();
        dockerHost = dockerComposeContainer.getServiceHost("azure-storage", 10000);
    }

    private static void initializeStorage() {
        try {
            cloudBlobClient =
                CloudStorageAccount.parse(String.format(STORAGE_CONN_STRING, dockerHost, 10000))
                .createCloudBlobClient();
        } catch (InvalidKeyException | URISyntaxException exception) {
            throw new RuntimeException("Unable to initialize storage", exception);
        }
    }

    public static void initialize() {
        createDocker();
        initializeStorage();
    }

    public static void stopDocker() {
        dockerComposeContainer.stop();
    }

    public void createBulkscanContainer() {
        try {
            testContainer = cloudBlobClient.getContainerReference(CONTAINER_NAME);
            testContainer.createIfNotExists();
        } catch (URISyntaxException | StorageException exception) {
            throw new RuntimeException("Unable to create container", exception);
        }
    }

    public void deleteBulkscanContainer() {
        try {
            testContainer.deleteIfExists();
        } catch (StorageException exception) {
            throw new RuntimeException("Unable to delete container", exception);
        }
    }

    public void upload(String directory) {
        try {
            CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(ZIP_FILE_NAME);

            // Blob need to be deleted as same blob may exists if previously uploaded blob was not deleted
            // due to doc upload failure
            if (blockBlobReference.exists()) {
                blockBlobReference.breakLease(0);
                blockBlobReference.delete();
            }

            byte[] fileContent = zipDir(directory);
            blockBlobReference.uploadFromByteArray(fileContent, 0, fileContent.length);
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
