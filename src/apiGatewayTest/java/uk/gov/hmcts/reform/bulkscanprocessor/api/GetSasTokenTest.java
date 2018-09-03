package uk.gov.hmcts.reform.bulkscanprocessor.api;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;

import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private static final String SUBSCRIPTION_KEY_HEADER_NAME = "Ocp-Apim-Subscription-Key";
    private static final String PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT = "testcert";

    private final Config config = ConfigFactory.load();

    @Test
    public void should_accept_request_with_valid_certificate_and_subscription_key() throws Exception {
        Response response =
            callSasTokenEndpoint(getValidClientKeyStore(), getValidSubscriptionKey())
                .thenReturn();

        assertThat(response.body().jsonPath().getString("sas_token")).isNotEmpty();
        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test
    public void should_reject_request_with_invalid_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            getValidClientKeyStore(),
            "invalid-subscription-key123"
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to invalid subscription key");
    }

    @Test
    public void should_reject_request_lacking_subscription_key() throws Exception {
        Response response = callSasTokenEndpoint(
            getValidClientKeyStore(),
            null
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).contains("Access denied due to missing subscription key");
    }

    @Test
    public void should_reject_request_with_unrecognised_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(
            getUnrecognisedClientKeyStore(),
            getValidSubscriptionKey()
        )
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    public void should_reject_request_lacking_client_certificate() throws Exception {
        Response response =
            callSasTokenEndpoint(null, getValidSubscriptionKey())
                .thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Missing client certificate");
    }

    @Test
    public void should_not_expose_http_version() {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl().replace("https://", "http://"))
            .header(SUBSCRIPTION_KEY_HEADER_NAME, getValidSubscriptionKey())
            .when()
            .get(getSasTokenEndpointPath())
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body().asString()).contains("Resource not found");
    }

    private Response callSasTokenEndpoint(
        KeyStoreWithPassword clientKeyStore,
        String subscriptionKey
    ) throws Exception {
        RequestSpecification request = RestAssured.given().baseUri(getApiGatewayUrl());

        if (clientKeyStore != null) {
            request = request.config(
                getSslConfigForClientCertificate(
                    clientKeyStore.keyStore,
                    clientKeyStore.password
                )
            );
        }

        if (subscriptionKey != null) {
            request = request.header(SUBSCRIPTION_KEY_HEADER_NAME, subscriptionKey);
        }

        return request.get(getSasTokenEndpointPath());
    }

    private RestAssuredConfig getSslConfigForClientCertificate(
        KeyStore clientKeyStore,
        String clientKeyStorePassword
    ) throws Exception {
        SSLConfig sslConfig = sslConfig()
            .allowAllHostnames()
            .sslSocketFactory(new SSLSocketFactory(clientKeyStore, clientKeyStorePassword));

        return RestAssured.config().sslConfig(sslConfig);
    }

    private KeyStoreWithPassword getValidClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.getString("client.valid-key-store.content"),
            config.getString("client.valid-key-store.password")
        );
    }

    private KeyStoreWithPassword getExpiredClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.getString("client.expired-key-store.content"),
            config.getString("client.expired-key-store.password")
        );
    }

    private KeyStoreWithPassword getNotYetValidClientKeyStore() throws Exception {
        return getClientKeyStore(
            config.getString("client.not-yet-valid-key-store.content"),
            config.getString("client.not-yet-valid-key-store.password")
        );
    }

    private KeyStoreWithPassword getClientKeyStore(String base64Content, String password) throws Exception {
        byte[] rawContent = Base64.getDecoder().decode(base64Content);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(rawContent), password.toCharArray());

        return new KeyStoreWithPassword(keyStore, password);
    }

    private KeyStoreWithPassword getUnrecognisedClientKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (
            InputStream keyStoreStream =
                this.getClass().getClassLoader().getResourceAsStream("unrecognised-client-certificate.pfx")
        ) {
            // loading from null stream would cause a quiet failure
            assertThat(keyStoreStream).isNotNull();

            keyStore.load(keyStoreStream, PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT.toCharArray());
        }

        return new KeyStoreWithPassword(keyStore, PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT);
    }

    private String getValidSubscriptionKey() {
        String subscriptionKey = config.getString("client.subscription-key");
        assertThat(subscriptionKey).as("Subscription key").isNotEmpty();
        return subscriptionKey;
    }

    private String getApiGatewayUrl() {
        String apiUrl = config.getString("api.gateway-url");
        assertThat(apiUrl).as("API gateway URL").isNotEmpty();
        return apiUrl;
    }

    private String getSasTokenEndpointPath() {
        return "/token/" + config.getString("container.name");
    }

    private static class KeyStoreWithPassword {
        public final KeyStore keyStore;
        public final String password;

        public KeyStoreWithPassword(KeyStore keyStore, String password) {
            this.keyStore = keyStore;
            this.password = password;
        }
    }
}
