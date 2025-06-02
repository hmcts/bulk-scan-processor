package uk.gov.hmcts.reform.bulkscanprocessor;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import static org.hamcrest.Matchers.equalTo;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.yaml")
public class CdamHealthTest {

    @Value("${test-cdam-url}")
    private String cdamUrl;

    @Test
    public void cdam_health_check() {
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(cdamUrl)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
            .get("/health")
            .then()
            .statusCode(200)
            .and()
            .body("status", equalTo("UP"));
    }
}
