package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.models.StorageErrorException;
import com.microsoft.rest.v2.Context;
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.net.MalformedURLException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private Config conf = ConfigFactory.load();

    private String testUrl;
    private String storageAccountUrl;
    private TestHelper testHelper;

    private String destZipFilename;

    private static final String zipFilename = "24-06-2018-00-00-00.test.zip";

    @Before
    public void setUp() throws MalformedURLException, InvalidKeyException {
        this.testUrl = conf.getString("test-url");

        this.testHelper = new TestHelper(
            conf.getString("test-storage-account-name"),
            conf.getString("test-storage-account-key"),
            conf.getString("test-storage-account-url")
        );
    }

    @After
    public void tearDown() {
        // cleanup previous runs
        if (!Strings.isNullOrEmpty(destZipFilename)) {
            BlockBlobURL blob = testHelper.getServiceURL().createContainerURL("bulkscan")
                .createBlockBlobURL(destZipFilename);
            blob.breakLease(0, null, Context.NONE);
            blob.delete(null, null, Context.NONE)
                .blockingGet();
        }
    }

    @Test
    public void should_return_sas_token_when_service_configuration_is_available() throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(this.testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .when().get("/token/bulkscan")
            .andReturn();

        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node = new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(
            String.valueOf(node.get("sas_token"))).build()
            .getQueryParams();

        Date tokenExpiry = DateUtil.parseDatetime(queryParams.get("se").get(0));
        assertThat(tokenExpiry).isNotNull();
        assertThat(queryParams.get("sig")).isNotNull(); //this is a generated hash of the resource string
        assertThat(queryParams.get("sv")).contains("2018-03-28"); //azure api version is latest
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
            .contains("No service configuration found for service doesnotexist");
    }

    @Test
    public void sas_token_should_have_read_and_write_capabilities_for_service() throws Exception {
        String sasToken = testHelper.getSasToken("test", this.testUrl);
        ServiceURL anonymousAccessServiceURL = testHelper.getAnonymousAccessServiceURL(this.storageAccountUrl);

        ContainerURL containerURL = anonymousAccessServiceURL.createContainerURL("bulkscan" + sasToken);

        destZipFilename = testHelper.getRandomFilename(zipFilename);
        testHelper.uploadAndLeaseZipFile(
            containerURL,
            Arrays.asList(
                "1111006.pdf"
            ),
            "1111006.metadata.json",
            destZipFilename
        );

        assertThat(testHelper.storageHasFile(containerURL, destZipFilename)).isTrue();
    }

    @Test(expected = StorageErrorException.class)
    public void sas_token_should_not_have_read_and_write_capabilities_for_other_service() throws Exception {
        String sasToken = testHelper.getSasToken("sscs", this.testUrl);

        ServiceURL anonymousAccessServiceURL = testHelper.getAnonymousAccessServiceURL(this.storageAccountUrl);

        ContainerURL containerURL = anonymousAccessServiceURL.createContainerURL("bulkscan" + sasToken);

        destZipFilename = testHelper.getRandomFilename(zipFilename);
        testHelper.uploadAndLeaseZipFile(
            containerURL,
            Collections.singletonList(
                "1111006.pdf"
            ),
            "1111006.metadata.json",
            destZipFilename
        );
    }

}
