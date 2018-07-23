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
import java.util.Optional;

import static io.restassured.config.SSLConfig.sslConfig;
import static org.assertj.core.api.Assertions.assertThat;

public class GetSasTokenTest {

    private static final String TEST_SERVICE_NAME = "non-existing-service";
    private static final String GET_SAS_TOKEN_PATH = "/token/" + TEST_SERVICE_NAME;
    private static final String PASSWORD_FOR_UNRECOGNISED_CLIENT_CERT = "testcert";

    private final Config config = ConfigFactory.load();

    @Test
    public void should_accept_request_with_valid_certificate() throws Exception {
        Response response =
            callSasTokenEndpoint(Optional.of(getValidClientKeyStore())).thenReturn();

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
    public void should_reject_request_with_unrecognised_client_certificate() throws Exception {
        Response response =
            callSasTokenEndpoint(Optional.of(getUnrecognisedClientKeyStore())).thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Invalid client certificate");
    }

    @Test
    public void should_reject_request_lacking_client_certificate() throws Exception {
        Response response = callSasTokenEndpoint(Optional.empty()).thenReturn();

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body().asString()).isEqualTo("Missing client certificate");
    }

    @Test
    public void should_not_expose_http_version() {
        Response response = RestAssured
            .given()
            .baseUri(getApiGatewayUrl().replace("https://", "http://"))
            .when()
            .get(GET_SAS_TOKEN_PATH)
            .thenReturn();

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body().asString()).contains("Resource not found");
    }

    private Response callSasTokenEndpoint(Optional<KeyStoreWithPassword> clientKeyStore) throws Exception {
        RequestSpecification request = RestAssured.given().baseUri(getApiGatewayUrl());

        if (clientKeyStore.isPresent()) {
            request = request.config(
                getSslConfigForClientCertificate(
                    clientKeyStore.get().keyStore,
                    clientKeyStore.get().password
                )
            );
        }

        return request.get(GET_SAS_TOKEN_PATH);
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
        byte[] clientKeyStore = Base64.getDecoder().decode(
            config.getString("client.key-store.content")
        );

        String clientKeyStorePassword = config.getString("client.key-store.password");

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(clientKeyStore), clientKeyStorePassword.toCharArray());

        return new KeyStoreWithPassword(keyStore, clientKeyStorePassword);
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

    private String getApiGatewayUrl() {
        String apiUrl = config.getString("api.gateway-url");
        assertThat(apiUrl).as("API gateway URL").isNotEmpty();
        return apiUrl;
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
