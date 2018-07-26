package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.base.Strings;
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

import java.util.UUID;
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
        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");
    }


    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        String srcZipFilename = "8_24-06-2018-00-00-00.zip";
        String destZipFilename = getRandomFilename(null, srcZipFilename);

        uploadZipFile(srcZipFilename, destZipFilename); // valid zip file

        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(destZipFilename), is(false));
    }

    @Test
    public void should_keep_zip_file_after_failed_processing() throws Exception {
        String srcZipFilename = "2_24-06-2018-00-00-00.zip";
        String destZipFilename = getRandomFilename(null, srcZipFilename);

        uploadZipFile(srcZipFilename, destZipFilename); // invalid due to missing json file

        // ensure that processing has happened
        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(destZipFilename), is(true));

        testContainer.getBlockBlobReference(destZipFilename).delete();
    }

    private void uploadZipFile(final String srcZipFilename, final String destZipFilename) throws Exception {
        byte[] zipFile = toByteArray(getResource(srcZipFilename));
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }

    private String getRandomFilename(String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(Strings.isNullOrEmpty(prefix) ? "" : prefix)
            .append(UUID.randomUUID().toString())
            .append(Strings.isNullOrEmpty(suffix) ? "" : suffix);
        return sb.toString();
    }

}
