package uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
public class SampleAppConsumerTest {

    private static final String TEST_S2S_TOKEN = "pact-test-s2s-token";

    @Pact(provider = "sample_app_ocr_validation", consumer = "bulk_scan_processor")
    public RequestResponsePact validOcrPact(PactDslWithProvider builder) throws Exception {
        return builder
            .uponReceiving("Request to validate valid OCR for type PERSONAL")
            .path("/forms/PERSONAL/validate-ocr")
            .method("POST")
            .body(loadJson("sampleapp/valid-ocr.json"))
            .headers(ImmutableMap.of("ServiceAuthorization", TEST_S2S_TOKEN))
            .willRespondWith()
            .status(200)
            .body("{ 'status' : 'SUCCESS', 'errors': [], 'warnings': [] }".replace("'", "\""))
            .toPact();
    }

    @Pact(provider = "sample_app_ocr_validation", consumer = "bulk_scan_processor")
    public RequestResponsePact invalidOcrPact(PactDslWithProvider builder) throws Exception {
        return builder
            .uponReceiving("Request to validate invalid OCR with missing mandatory field 'last_name' for type PERSONAL")
            .path("/forms/PERSONAL/validate-ocr")
            .method("POST")
            .body(loadJson("sampleapp/ocr-with-missing-last-name.json"))
            .headers(ImmutableMap.of("ServiceAuthorization", TEST_S2S_TOKEN))
            .willRespondWith()
            .status(200)
            .body("{ 'status' : 'SUCCESS', 'errors': ['last_name is missing'], 'warnings': [] }".replace("'", "\""))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "validOcrPact")
    public void should_handle_valid_ocr(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(ImmutableMap.of("ServiceAuthorization", TEST_S2S_TOKEN))
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(loadJson("sampleapp/valid-ocr.json"))
            .when()
            .post(mockServer.getUrl() + "/forms/PERSONAL/validate-ocr")
            .then()
            .statusCode(200)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("status")).isEqualTo("SUCCESS");
        assertThat(response.getList("errors")).isEmpty();
        assertThat(response.getList("warnings")).isEmpty();
    }

    @Test
    @PactTestFor(pactMethod = "invalidOcrPact")
    public void should_handle_invalid_ocr(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(ImmutableMap.of("ServiceAuthorization", TEST_S2S_TOKEN))
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(loadJson("sampleapp/ocr-with-missing-last-name.json"))
            .when()
            .post(mockServer.getUrl() + "/forms/PERSONAL/validate-ocr")
            .then()
            .statusCode(200)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("status")).isEqualTo("SUCCESS");
        assertThat(response.getList("errors")).containsExactly("last_name is missing");
        assertThat(response.getList("warnings")).isEmpty();
    }

    private String loadJson(String path) throws Exception {
        return Resources.toString(Resources.getResource(path), Charsets.UTF_8);
    }
}
