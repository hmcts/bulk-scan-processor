package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;

public class BlobProcessorTest {

    private String testUrl;

    private long scanDelay;

    private String accountName;

    private String testStorageAccountKey;

    private String s2sUrl;

    private String s2sName;

    private String s2sSecret;

    private CloudBlobContainer testContainer;

    private CloudBlobContainer testSasContainer;

    private String blobContainerUrl;

    private TestHelper testHelper;

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();
        this.testUrl = conf.getString("test-url");
        this.scanDelay = Long.parseLong(conf.getString("test-scan-delay"));
        this.accountName = conf.getString("test-storage-account-name");
        this.testStorageAccountKey = conf.getString("test-storage-account-key");
        this.s2sUrl = conf.getString("test-s2s-url");
        this.s2sName = conf.getString("test-s2s-name");
        this.s2sSecret = conf.getString("test-s2s-secret");
        this.blobContainerUrl = "https://" + this.accountName + ".blob.core.windows.net/";

        this.testHelper = new TestHelper();

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");

        String sasToken = testHelper.getSasToken("test", this.testUrl);
        testSasContainer = testHelper.getCloudContainer(sasToken, "test", this.blobContainerUrl);
    }

    @Test
    public void should_process_zipfile_after_upload_and_set_status() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf", "1111002.pdf");
        String metadataFile = "1111006_2.metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.zip");

        testHelper.uploadZipFile(testContainer, files, metadataFile, destZipFilename); // valid zip file

        await()
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(false));

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        EnvelopeMetadataResponse envelopeMetadataResponse =
            testHelper.getEnvelopes(this.testUrl, s2sToken, Status.PROCESSED);

        // some test DBs are not cleaned so there will probably be more than 1
        assertThat(envelopeMetadataResponse.envelopes.size()).isGreaterThanOrEqualTo(1);

        assertThat(envelopeMetadataResponse.envelopes)
            .extracting("zipFileName", "status")
            .containsOnlyOnce(tuple(destZipFilename, Status.PROCESSED));


        List<Envelope> envelopes = envelopeMetadataResponse.envelopes
            .stream()
            .filter(e -> destZipFilename.equals(e.getZipFileName()))
            .collect(Collectors.toList());
        assertThat(envelopes.size()).isEqualTo(1);
        assertThat(envelopes.get(0).getScannableItems().size()).isEqualTo(2);

        assertThat(envelopes.get(0).getScannableItems())
            .extracting("documentUrl").noneMatch(ObjectUtils::isEmpty);
        assertThat(envelopes.get(0).getScannableItems())
            .extracting("fileName").containsExactlyInAnyOrder("1111006.pdf", "1111002.pdf");
    }

}
