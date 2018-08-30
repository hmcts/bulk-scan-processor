package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest {

    private transient long scanDelay;
    private transient CloudBlobContainer testContainer;
    private List<String> filesToDeleteAfterTest = new ArrayList<>();
    private transient TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));

        StorageCredentialsAccountAndKey credentials =
            new StorageCredentialsAccountAndKey(
                conf.getString("test-storage-account-name"),
                conf.getString("test-storage-account-key")
            );

        testContainer = new CloudStorageAccount(credentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");

        testHelper = new TestHelper();
    }

    @After
    public void tearDown() throws Exception {
        for (String filename: filesToDeleteAfterTest) {
            testContainer.getBlockBlobReference(filename).deleteIfExists();
        }
    }

    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf");
        String metadataFile = "1111006.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.zip");

        testHelper.uploadZipFile(testContainer, files, metadataFile, destZipFilename); // valid zip file
        filesToDeleteAfterTest.add(destZipFilename);

        await("file should be deleted")
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .pollInterval(1, TimeUnit.SECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(false));

        assertThat(testHelper.storageHasFile(testContainer, destZipFilename)).isFalse();
    }

    @Test
    public void should_keep_zip_file_after_failed_processing() throws Exception {
        String srcZipFilename = "2_24-06-2018-00-00-00.zip";
        String destZipFilename = testHelper.getRandomFilename(srcZipFilename.substring(2));

        testHelper.uploadZipFile(testContainer, srcZipFilename, destZipFilename); // invalid due to missing json file
        filesToDeleteAfterTest.add(destZipFilename);

        await("file should not be deleted")
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .pollDelay(scanDelay * 2, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(true));

        // 0 implies immediately breaking the lease
        // Lease needs to broken before deleting file
        testContainer.getBlockBlobReference(destZipFilename).breakLease(0);
        testContainer.getBlockBlobReference(destZipFilename).delete();

        assertThat(testHelper.storageHasFile(testContainer, destZipFilename)).isFalse();
    }
}
