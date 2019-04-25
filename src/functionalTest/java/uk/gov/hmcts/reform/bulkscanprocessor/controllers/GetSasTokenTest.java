package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.StorageUri;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.util.DateUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private Config conf = ConfigFactory.load();

    private String testUrl;
    private String blobContainerUrl;
    private TestHelper testHelper;

    private String destZipFilename;

    private String testPrivateKeyDer;

    private static final String zipFilename = "24-06-2018-00-00-00.test.zip";

    @Before
    public void setUp() {
        this.testUrl = conf.getString("test-url");
        this.blobContainerUrl = conf.getString("test-storage-account-url") + "/";
        this.testPrivateKeyDer = conf.getString("test-private-key-der");

        this.testHelper = new TestHelper();
    }

    @After
    public void tearDown() throws Exception {
        // cleanup previous runs
        if (!Strings.isNullOrEmpty(destZipFilename)) {
            StorageCredentialsAccountAndKey storageCredentials =
                new StorageCredentialsAccountAndKey(
                    conf.getString("test-storage-account-name"),
                    conf.getString("test-storage-account-key")
                );

            CloudBlobContainer testContainer = new CloudStorageAccount(
                storageCredentials,
                new StorageUri(new URI(conf.getString("test-storage-account-url")), null),
                null,
                null
            )
                .createCloudBlobClient()
                .getContainerReference(conf.getString("test-storage-container-name"));

            CloudBlockBlob blob = testContainer.getBlockBlobReference(destZipFilename);
            if (blob.exists()) {
                blob.breakLease(0);
                blob.deleteIfExists();
            }
        }
    }

    @Test
    public void should_return_sas_token_for_sscs_when_service_configuration_is_available() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/sscs")
            .andReturn();

        verifySasTokenProperties(tokenResponse);
    }

    @Test
    public void should_return_sas_token_for_finrem_when_service_configuration_is_available() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/finrem")
            .andReturn();

        verifySasTokenProperties(tokenResponse);
    }

    @Test
    public void should_return_sas_token_for_probate_when_service_configuration_is_available() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/probate")
            .andReturn();

        verifySasTokenProperties(tokenResponse);
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
            .contains("No service configuration found for service doesnotexist");
    }

    @Test
    public void sas_token_should_have_read_and_write_capabilities_for_service() throws Exception {
        String testContainerName = conf.getString("test-storage-container-name");
        String sasToken = testHelper.getSasToken(testContainerName, this.testUrl);
        CloudBlobContainer testSasContainer =
            testHelper.getCloudContainer(sasToken, testContainerName, this.blobContainerUrl);

        destZipFilename = testHelper.getRandomFilename(zipFilename);
        testHelper.uploadAndLeaseZipFile(
            testSasContainer,
            Arrays.asList(
                "1111006.pdf"
            ),
            "1111006.metadata.json",
            destZipFilename,
            testPrivateKeyDer
        );

        assertThat(testHelper.storageHasFile(testSasContainer, destZipFilename)).isTrue();
    }

    @Test(expected = StorageException.class)
    public void sas_token_should_not_have_read_and_write_capabilities_for_other_service() throws Exception {
        String sasToken = testHelper.getSasToken("sscs", this.testUrl);
        CloudBlobContainer testSasContainer =
            testHelper.getCloudContainer(sasToken, "test", this.blobContainerUrl);

        destZipFilename = testHelper.getRandomFilename(zipFilename);
        testHelper.uploadAndLeaseZipFile(
            testSasContainer,
            Arrays.asList(
                "1111006.pdf"
            ),
            "1111006.metadata.json",
            destZipFilename,
            testPrivateKeyDer
        );
    }

    private void verifySasTokenProperties(Response tokenResponse) throws java.io.IOException, StorageException {
        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node = new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        Map<String, String[]> queryParams = PathUtility.parseQueryString(node.get("sas_token").asText());

        Date tokenExpiry = DateUtil.parseDatetime(queryParams.get("se")[0]);
        assertThat(tokenExpiry).isNotNull();
        assertThat(queryParams.get("sig")).isNotNull(); //this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2018-11-09"); //azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl"); //access permissions(write-w,list-l)
    }
}
