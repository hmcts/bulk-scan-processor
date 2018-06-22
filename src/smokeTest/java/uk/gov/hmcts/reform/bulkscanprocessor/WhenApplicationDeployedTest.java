package uk.gov.hmcts.reform.bulkscanprocessor;

import io.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

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
            .get("/info")
            .then()
            .body("containers", not(emptyArray()));
    }
}
