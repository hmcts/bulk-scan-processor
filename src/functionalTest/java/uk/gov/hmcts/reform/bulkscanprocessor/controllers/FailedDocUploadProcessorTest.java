package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

public class FailedDocUploadProcessorTest {

    private String testUrl;
    private long reuploadDelay;
    private long scanDelay;
    private String s2sUrl;
    private String s2sName;
    private String s2sSecret;
    private CloudBlobContainer testContainer;
    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();

        this.testUrl = conf.getString("test-url");
        this.reuploadDelay = Long.parseLong(conf.getString("test-reupload-delay"));
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));
        this.s2sUrl = conf.getString("test-s2s-url");
        this.s2sName = conf.getString("test-s2s-name");
        this.s2sSecret = conf.getString("test-s2s-secret");

        this.testHelper = new TestHelper();

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(
                conf.getString("test-storage-account-name"),
                conf.getString("test-storage-account-key")
            );

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");
    }

    @Test
    public void should_process_zipfile_after_upload_failure_and_set_status() throws Exception {
        String metadataFile = "1111006_2.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.zip");

        // let us upload incomplete set of pdfs
        testHelper.uploadZipFile(
            testContainer,
            Collections.singletonList("1111006.pdf"),
            metadataFile,
            destZipFilename
        );

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        await()
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .until(() -> testHelper
                .getEnvelopes(this.testUrl, s2sToken, Status.UPLOAD_FAILURE)
                .envelopes
                .stream()
                .map(EnvelopeResponse::getZipFileName)
                .anyMatch(destZipFilename::equals)
            );

        List<String> files = Arrays.asList("1111006.pdf", "1111002.pdf");

        // valid zip file with all the pdfs in it
        testHelper.uploadZipFile(testContainer, files, metadataFile, destZipFilename);

        await()
            .atMost(reuploadDelay + 15_000, TimeUnit.MILLISECONDS)
            .until(() -> !testHelper.storageHasFile(testContainer, destZipFilename));

        assertThat(testHelper.getEnvelopes(this.testUrl, s2sToken, Status.PROCESSED).envelopes)
            .extracting("zipFileName")
            .containsOnlyOnce(destZipFilename);
    }

}
