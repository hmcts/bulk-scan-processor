package uk.gov.hmcts.reform.bulkscanprocessor;

import io.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.not;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.yaml")
public class WhenApplicationDeployedTest {

    @Value("${test-url}")
    private String testUrl;

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
}
