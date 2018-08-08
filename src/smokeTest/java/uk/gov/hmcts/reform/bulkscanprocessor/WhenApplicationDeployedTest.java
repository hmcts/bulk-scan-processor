package uk.gov.hmcts.reform.bulkscanprocessor;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.TestHelper;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.yaml")
public class WhenApplicationDeployedTest {

    @Value("${test-url}")
    private String testUrl;

    @Value("${test-s2s-name}")
    private String s2sName;

    @Value("${test-s2s-secret}")
    private String s2sSecret;

    @Value("${test-s2s-url}")
    private String s2sUrl;

    @Test
    public void storage_container_exists() {
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .get("/info")
            .then()
            .body("containers", not(emptyArray()));
    }

    @Test
    public void can_retrieve_envelopes() {
        TestHelper testHelper = new TestHelper();
        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        Response response = testHelper.getAllProcessedEnvelopesResponse(this.testUrl, s2sToken);
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

}
