package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.assertj.core.util.DateUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.TestHelper;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetSasTokenTest extends BaseFunctionalTest  {

    private Config conf = ConfigFactory.load();

    private String testUrl;
    private String blobContainerUrl;
    private TestHelper testHelper;

    private String destZipFilename;
    private String leaseId;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.testUrl = conf.getString("test-url");
        this.blobContainerUrl = conf.getString("test-storage-account-url") + "/";

        this.testHelper = new TestHelper();
        this.leaseId = null;

    }

    @AfterEach
    public void tearDown() {
        // cleanup previous runs
        if (!Strings.isNullOrEmpty(destZipFilename)) {
            BlobClient blobClient = inputContainer.getBlobClient(destZipFilename);
            if (blobClient.exists()) {
                if(!Strings.isNullOrEmpty(leaseId)){
                    blobClient.deleteWithResponse(
                        DeleteSnapshotsOptionType.INCLUDE,
                        new BlobRequestConditions().setLeaseId(leaseId),
                        null,
                        Context.NONE);
                }
                else{
                    blobClient.delete();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "sscs", "finrem", "probate", "publiclaw" })
    public void should_return_sas_token_when_service_configuration_is_available(String container) throws Exception {
        verifySasTokenProperties(sendSasTokenRequest(container));
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        Response tokenResponse = sendSasTokenRequest("doesnotexist");
        assertThat(tokenResponse.getStatusCode()).isEqualTo(400);
        assertThat(tokenResponse.getBody().asString())
            .contains("No service configuration found for service doesnotexist");
    }

    @Test
    public void sas_token_should_have_read_and_write_capabilities_for_service() throws Exception {
        String testContainerName = conf.getString("test-storage-container-name");
        String sasToken = testHelper.getSasToken(testContainerName, this.testUrl);
        BlobContainerClient testSasContainer =
            testHelper.getContainerClient(sasToken, testContainerName, this.blobContainerUrl);

        destZipFilename = testHelper.getRandomFilename();
        this.leaseId = testHelper.uploadAndLeaseZipFile(
            testSasContainer,
            Arrays.asList(
                "1111006.pdf"
            ),
            "exception_metadata.json",
            destZipFilename
        );

        assertThat(testHelper.storageHasFile(testSasContainer, destZipFilename)).isTrue();
    }

    @Test
    public void sas_token_should_not_have_read_and_write_capabilities_for_other_service() throws Exception {
        String sasToken = testHelper.getSasToken("sscs", this.testUrl);
        BlobContainerClient testSasContainer =
            testHelper.getContainerClient(sasToken, "test", this.blobContainerUrl);

        destZipFilename = testHelper.getRandomFilename();
        assertThrows(
            BlobStorageException.class,
            () -> testHelper.uploadAndLeaseZipFile(
                    testSasContainer,
                    Arrays.asList(
                            "1111006.pdf"
                    ),
                    "exception_metadata.json",
                    destZipFilename
            )
        );
    }

    private void verifySasTokenProperties(Response tokenResponse) throws java.io.IOException {
        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node = new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);

        Map<String, String> queryParams = URLEncodedUtils
            .parse(node.get("sas_token").asText(), Charset.forName("UTF-8")).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));


        Date tokenExpiry = DateUtil.parseDatetime(queryParams.get("se"));
        assertThat(tokenExpiry).isNotNull();
        assertThat(queryParams.get("sig")).isNotNull(); //this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2019-12-12"); //azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl"); //access permissions(write-w,list-l)
    }

    private Response sendSasTokenRequest(String container) {
        return RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(this.testUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor functional test")
                .when().get("/token/" + container)
                .andReturn();
    }
}
