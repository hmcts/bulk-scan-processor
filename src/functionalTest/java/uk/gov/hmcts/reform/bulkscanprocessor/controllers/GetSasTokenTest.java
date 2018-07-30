package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Map;
import java.util.stream.StreamSupport;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private String testUrl;

    private String accountName;

    private String testStorageAccountKey;

    private static final String zipFilename = "8_24-06-2018-00-00-00.zip";

    @Before
    public void setUp() throws Exception {
        Config conf = ConfigFactory.load();
        this.testUrl = conf.getString("test-url");
        this.accountName = conf.getString("test-storage-account-name");
        this.testStorageAccountKey = conf.getString("test-storage-account-key");

        StorageCredentialsAccountAndKey storageCredentials =
            new StorageCredentialsAccountAndKey(accountName, testStorageAccountKey);

        CloudBlobContainer testContainer = new CloudStorageAccount(storageCredentials, true)
            .createCloudBlobClient()
            .getContainerReference("test");

        testContainer.getBlockBlobReference(zipFilename).delete();
    }



    @Test
    public void should_return_sas_token_when_service_configuration_is_available() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/sscs")
            .andReturn();

        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node = new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        Map<String, String[]> queryParams = PathUtility.parseQueryString(node.get("sas_token").asText());

        Date tokenExpiry = DateUtil.parseDatetime(queryParams.get("se")[0]);
        assertThat(tokenExpiry).isNotNull();
        assertThat(queryParams.get("sig")).isNotNull(); //this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2017-07-29"); //azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl"); //access permissions(write-w,list-l)

    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/doesnotexist")
            .andReturn();

        assertThat(tokenResponse.getStatusCode()).isEqualTo(400);
        assertThat(tokenResponse.getBody().asString())
            .isEqualTo("No service configuration found for service doesnotexist");
    }

    @Test
    public void sas_token_should_have_read_and_write_capabilities_for_service() throws Exception {
        String sasToken = getSasToken("test");
        CloudBlobContainer testSasContainer = getCloudContainer(sasToken, "test");

        uploadZipFile(zipFilename, testSasContainer);
        assertThat(storageHasFile(zipFilename, testSasContainer)).isTrue();
    }

    @Test(expected = InvalidKeyException.class)
    public void sas_token_should_not_have_read_and_write_capabilities_for_other_service() throws Exception {
        String sasToken = getSasToken("sscs");
        CloudBlobContainer testSasContainer = getCloudContainer(sasToken, "test");

        uploadZipFile(zipFilename, testSasContainer);
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

    private void uploadZipFile(final String zipName, final CloudBlobContainer container) throws Exception {
        byte[] zipFile = toByteArray(getResource(zipName));
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(zipName);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    private boolean storageHasFile(String fileName, final CloudBlobContainer container) {
        return StreamSupport.stream(container.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }

}
