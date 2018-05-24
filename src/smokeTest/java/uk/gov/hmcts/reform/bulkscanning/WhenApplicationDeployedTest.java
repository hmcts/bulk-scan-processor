package uk.gov.hmcts.reform.bulkscanning;

import io.restassured.RestAssured;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.equalTo;

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
            .body("container_exists", equalTo(true));
    }
}
