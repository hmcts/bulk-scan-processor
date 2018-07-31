package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

public class DocumentUploadTest {

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

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadTest.class);

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
    public void should_process_document_after_upload_and_set_status_uploaded() throws Exception {
        List<String> files = Arrays.asList("1111006.pdf");
        String metadataFile = "1111006.metadata.json";
        String destZipFilename = testHelper.getRandomFilename(null, "8_24-06-2018-00-00-00.zip");

        testHelper.uploadZipFile(testContainer, files, metadataFile, destZipFilename); // valid zip file

        await()
            .atMost(scanDelay + 15_000, TimeUnit.MILLISECONDS)
            .until(() -> testHelper.storageHasFile(testContainer, destZipFilename), is(false));

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        log.warn("s2s token: [{}]", s2sToken);

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", "Bearer " + s2sToken)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/envelopes")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        log.warn("Envelopes response body: [{}]", response.getBody().print());

        EnvelopeMetadataResponse envelopeMetadataResponse =
            response.getBody().as(EnvelopeMetadataResponse.class, ObjectMapperType.JACKSON_2);

        assertThat(envelopeMetadataResponse.envelopes.size()).isEqualTo(1);

        assertThat(envelopeMetadataResponse.envelopes)
            .extracting("zipFileName", "status")
            .containsExactlyInAnyOrder(tuple(destZipFilename, DOC_UPLOADED));

        assertThat(envelopeMetadataResponse.envelopes)
            .extracting("document_url")
            .hasSize(1)
            .doesNotContainNull();
    }

}
