package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeMetadataResponse;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
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

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");

        String sasToken = getSasToken("test");
        testSasContainer = getCloudContainer(sasToken, "test");
    }

    @Test
    public void should_process_document_after_upload_and_set_status_uploaded() throws Exception {
        String zipFilename = "1_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(zipFilename);

        await()
            .atMost(scanDelay + 10000, TimeUnit.MILLISECONDS)
            .until(() -> storageHasFile(zipFilename), is(false));

        String s2sToken = signIn();

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("ServiceAuthorization", s2sToken)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/envelopes")
            .andReturn();

        EnvelopeMetadataResponse envelopeMetadataResponse = response.getBody().as(EnvelopeMetadataResponse.class);

        assertThat(envelopeMetadataResponse.envelopes.size()).isEqualTo(1);

        assertThat(envelopeMetadataResponse.envelopes)
            .extracting("zipFileName", "status")
            .containsExactlyInAnyOrder(tuple("1_24-06-2018-00-00-00.zip", DOC_UPLOADED));

        assertThat(envelopeMetadataResponse.envelopes)
            .extracting("document_url")
            .hasSize(2)
            .doesNotContainNull();
    }


    protected String signIn() {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", this.s2sName,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(this.s2sSecret)
        );

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.s2sUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(params)
            .when()
            .post("/lease")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response
            .getBody()
            .print();
    }

    // TODO next 2 methods duplicated, refactor to test utilities
    private void uploadZipToBlobStore(String fileName) throws Exception {
        byte[] zipFile = toByteArray(getResource(fileName));

        CloudBlockBlob blockBlobReference = testSasContainer.getBlockBlobReference(fileName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName) {
        return StreamSupport.stream(testSasContainer.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }

    private String getSasToken(String containerName) throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor functional test")
            .when().get("/token/" + containerName)
            .andReturn();

        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node =
            new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        return node.get("sas_token").asText();
    }

    private CloudBlobContainer getCloudContainer(String sasToken, String containerName) throws Exception {
        final StorageCredentials creds =
            new StorageCredentialsSharedAccessSignature(sasToken);
        return new CloudStorageAccount(creds, true)
            .createCloudBlobClient()
            .getContainerReference(containerName);
    }

}
