package uk.gov.hmcts.reform.bulkscanprocessor.api;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private static final String SUBSCRIPTION_KEY_HEADER_NAME = "Ocp-Apim-Subscription-Key";
    private static final String TEST_SERVICE_NAME = "non-existing-service";
    private static final String GET_SAS_TOKEN_PATH = "/token/" + TEST_SERVICE_NAME;

    private final Config config = ConfigFactory.load();

    @Test
    public void should_accept_request_with_valid_subscription_key() throws Exception {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl())
            .header(SUBSCRIPTION_KEY_HEADER_NAME, getValidSubscriptionKey())
            .get(GET_SAS_TOKEN_PATH)
            .thenReturn();

        // As the client service is not configured, the SAS endpoint will reject the request.
        // However, that's enough to know the API gateway let the request through.
        assertThat(response.getStatusCode()).isEqualTo(400);

        String expectedBody = String.format(
            "No service configuration found for service %s",
            TEST_SERVICE_NAME
        );

        assertThat(response.body().asString()).isEqualTo(expectedBody);
    }

    @Test
    public void should_reject_request_with_invalid_subscription_key() throws Exception {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl())
            .header(SUBSCRIPTION_KEY_HEADER_NAME, "invalid-subscription-key")
            .get(GET_SAS_TOKEN_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Test
    public void should_reject_request_lacking_subscription_key() throws Exception {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl())
            .get(GET_SAS_TOKEN_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    @Test
    public void should_not_expose_http_version() throws Exception {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl().replace("https://", "http://"))
            .header(SUBSCRIPTION_KEY_HEADER_NAME, getValidSubscriptionKey())
            .get(GET_SAS_TOKEN_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body().asString()).contains("Resource not found");
    }

    private String getValidSubscriptionKey() throws Exception {
        String clientSubscriptionKey = config.getString("client.subscription-key");
        assertThat(clientSubscriptionKey).as("Subscription key").isNotEmpty();
        return clientSubscriptionKey;
    }

    private String getApiGatewayUrl() {
        String apiUrl = config.getString("api.gateway-url");
        assertThat(apiUrl).as("API gateway URL").isNotEmpty();
        return apiUrl;
    }
}
