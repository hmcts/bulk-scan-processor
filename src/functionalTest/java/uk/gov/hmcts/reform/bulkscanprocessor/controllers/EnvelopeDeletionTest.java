package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest {

    private long scanDelay;
    private CloudBlobContainer inputContainer;
    private CloudBlobContainer rejectedContainer;
    private List<String> filesToDeleteAfterTest = new ArrayList<>();
    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(
                conf.getString("test-storage-account-name"),
                conf.getString("test-storage-account-key")
            );

        CloudBlobClient cloudBlobClient = new CloudStorageAccount(
            storageCredentials,
            new StorageUri(new URI(conf.getString("test-storage-account-url")), null),
            null,
            null
        )
            .createCloudBlobClient();

        String inputContainerName = conf.getString("test-storage-container-name");
        String rejectedContainerName = inputContainerName + "-rejected";

        inputContainer = cloudBlobClient.getContainerReference(inputContainerName);
        rejectedContainer = cloudBlobClient.getContainerReference(rejectedContainerName);

        testHelper = new TestHelper();
    }

    @After
    public void tearDown() throws Exception {
        for (String filename: filesToDeleteAfterTest) {
            try {
                inputContainer.getBlockBlobReference(filename).breakLease(0);
            } catch (StorageException e) {
                // Do nothing as the file was not leased
            }

            inputContainer.getBlockBlobReference(filename).deleteIfExists();
            rejectedContainer.getBlockBlobReference(filename).deleteIfExists();
        }
    }

    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf");
        String metadataFile = "1111006.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(inputContainer, files, metadataFile, destZipFilename); // valid zip file
        filesToDeleteAfterTest.add(destZipFilename);

        await("file should be deleted")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(inputContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(rejectedContainer, destZipFilename)).isFalse();
    }

    @Test
    public void should_move_invalid_zip_file_to_rejected_container() throws Exception {
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        testHelper.uploadZipFile(
            inputContainer,
            Arrays.asList("1111006.pdf"),
            null, // missing metadata file
            destZipFilename
        );

        filesToDeleteAfterTest.add(destZipFilename);

        await("file should be deleted")
            .atMost(scanDelay + 40_000, TimeUnit.MILLISECONDS)
            .pollInterval(2, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(inputContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(rejectedContainer, destZipFilename)).isTrue();
    }
}
