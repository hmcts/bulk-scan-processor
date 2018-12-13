package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationFailingResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

@ActiveProfiles("wiremock")
@AutoConfigureWireMock
@ContextConfiguration(initializers = ErrorNotificationClientTest.ClientContextInitialiser.class)
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ErrorNotificationClientTest {

    @Autowired
    private ErrorNotificationClient client;

    @Autowired
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        // this will go away once request body is implemented
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Test
    public void should_return_Created_when_everything_is_ok_with_request() throws JsonProcessingException {
        // given
        ErrorNotificationResponse response = new ErrorNotificationResponse("some id");
        stubWithResponse(created().withBody(mapper.writeValueAsBytes(response)));

        // when
        ErrorNotificationResponse notificationResponse = client.notify(new ErrorNotificationRequest());

        // then
        assertThat(notificationResponse).isEqualToComparingFieldByField(response);
    }

    @Test
    public void should_return_NotificationClientException_when_badly_authorised() {
        // given
        stubWithResponse(unauthorized());

        // when
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest()));

        // then
        assertThat(throwable).isInstanceOf(NotificationClientException.class);

        // and
        NotificationClientException exception = (NotificationClientException) throwable;

        assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
        assertThat(exception.getResponse()).isNull();
    }

    @Test
    public void should_return_NotificationClientException_with_multiple_missing_required_fields_info()
        throws JsonProcessingException {
        // given
        String message = "po_box is required | error_description is required";
        stubWithResponse(getBadRequest(message));

        // when
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest()));

        // then
        assertThrowingResponse(throwable, message);
    }

    private void stubWithResponse(ResponseDefinitionBuilder builder) {
        stubFor(post("/notifications").willReturn(builder));
    }

    private ResponseDefinitionBuilder getBadRequest(String bodyMessage) throws JsonProcessingException {
        return badRequest()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBody(get4xxResponseBody(bodyMessage));
    }

    private byte[] get4xxResponseBody(String message) throws JsonProcessingException {
        return mapper.writeValueAsBytes(new ErrorNotificationFailingResponse(message));
    }

    private void assertThrowingResponse(Throwable throwable, String message) {
        assertThat(throwable).isInstanceOf(NotificationClientException.class);

        // and
        NotificationClientException exception = (NotificationClientException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo(message);
    }

    static class ClientContextInitialiser implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.setProperty("wiremock.port", Integer.toString(findAvailableTcpPort()));
        }

        @Bean
        public Options options(@Value("${wiremock.port}") int port) {
            return WireMockConfiguration.options().port(port);
        }
    }
}
