package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;

public class EnvelopeDeletionTest {

    private transient long scanDelay;

    private transient String accountName;

    private transient String testStorageKey;

    private transient CloudBlobContainer testContainer;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load("application.properties");
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));
        this.accountName = conf.getString("test-storage-account-name");
        this.testStorageKey = conf.getString("test-storage-account-key");

        StorageCredentialsAccountAndKey credentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageKey);

        testContainer = new CloudStorageAccount(credentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");
    }


    @Test
    public void should_delete_zip_file_after_successful_ingestion() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf");
        String metadataFile = "1111006.metadata.json";
        String destZipFilename = getRandomFilename(null, "8_24-06-2018-00-00-00.zip");

        uploadZipFile(files, metadataFile, destZipFilename); // valid zip file

        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(destZipFilename), is(false));
        Assert.assertThat("File has not been deleted.",
            storageHasFile(destZipFilename), is(false));
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
        Assert.assertThat("File not found.",
            storageHasFile(destZipFilename), is(false));
    }

    private void uploadZipFile(final String srcZipFilename, final String destZipFilename) throws Exception {
        byte[] zipFile = Resources.toByteArray(Resources.getResource(srcZipFilename));
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private void uploadZipFile(List<String> files, String metadataFile, final String destZipFilename) throws Exception {
        byte[] zipFile = createZipArchiveWithRandomName(files, metadataFile, destZipFilename);
        CloudBlockBlob blockBlobReference = testContainer.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }

    private String getRandomFilename(String prefix, String suffix) {
        StringBuilder strBuffer = new StringBuilder();
        strBuffer.append(Strings.isNullOrEmpty(prefix) ? "" : prefix)
            .append(UUID.randomUUID().toString())
            .append(Strings.isNullOrEmpty(suffix) ? "" : suffix);
        return strBuffer.toString();
    }

    private byte[] createZipArchiveWithRandomName(
        List<String> files, String metadataFile, String zipFilename
    ) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            for (String file : files) {
                zos.putNextEntry(new ZipEntry(file));
                zos.write(Resources.toByteArray(Resources.getResource(file)));
                zos.closeEntry();
            }
            String metadataTemplate =
                Resources.toString(Resources.getResource(metadataFile), StandardCharsets.UTF_8);
            String metadata = metadataTemplate.replace("$$zip_file_name$$", zipFilename);
            zos.putNextEntry(new ZipEntry("metadata.json"));
            zos.write(metadata.getBytes());
            zos.closeEntry();
        } finally {
            zos.close();
        }
        return outputStream.toByteArray();
    }


}
