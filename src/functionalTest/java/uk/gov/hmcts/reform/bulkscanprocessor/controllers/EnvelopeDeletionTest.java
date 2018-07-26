package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
public class EnvelopeDeletionTest {

    @Value("${test-scan-delay}")
    private long scanDelay;

    @Value("${test-storage-account-name}")
    private String accountName;

    @Value("${test-storage-account-key}")
    private String testStorageAccountKey;

    private CloudBlobContainer testContainer;

    @Before
    public void setUp() throws Exception {
        // Note: this test uses Azure blob store credentials instead of a Sas token
        // as it needs to delete files in its unhappy path. Sas tokens have only the
        // following permissions: write and list
        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");
    }


    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        String zipFilename = "8_24-06-2018-00-00-00.zip";

        uploadZipFile(zipFilename); // valid zip file

        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(zipFilename), is(false));
    }

    @Test
    public void should_keep_zip_file_after_failed_processing() throws Exception {
        String zipFilename = "2_24-06-2018-00-00-00.zip";

        uploadZipFile(zipFilename); // invalid due to missing json file

        // ensure that processing has happened
        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(zipFilename), is(true));

        testContainer.getBlockBlobReference(zipFilename).delete();
    }

    private void uploadZipFile(final String zipName) throws Exception {
        byte[] zipFile = toByteArray(getResource(zipName));
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(zipName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }
}
