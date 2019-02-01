package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.revinate.assertj.json.JsonPathAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

@ActiveProfiles("wiremock")
@AutoConfigureWireMock
@ContextConfiguration(initializers = ErrorNotificationClientTest.ClientContextInitialiser.class)
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ErrorNotificationClientTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Autowired
    private ErrorNotificationClient client;

    @Autowired
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        capture.reset();
    }

    @After
    public void cleanUp() {
        capture.flush();
    }

    @Test
    public void should_return_Created_when_everything_is_ok_with_request() throws JsonProcessingException {
        // given
        ErrorNotificationResponse response = new ErrorNotificationResponse("some id");
        stubWithResponse(created().withBody(mapper.writeValueAsBytes(response)));
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            ERR_METAFILE_INVALID.name(),
            "test_error_description"
        );

        // when
        ErrorNotificationResponse notificationResponse = client.notify(request);

        // then
        assertThat(notificationResponse).isEqualToComparingFieldByField(response);

        // and
        assertRequestLogs(request);
    }

    @Test
    public void should_return_NotificationClientException_when_badly_authorised() {
        // given
        stubWithResponse(unauthorized());
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            ERR_METAFILE_INVALID.name(),
            "test_error_description"
        );

        // when
        Throwable throwable = catchThrowable(() -> client.notify(request));

        // then
        assertThat(throwable).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException exception = (ErrorNotificationException) throwable;

        assertThat(exception.getStatus()).isEqualTo(UNAUTHORIZED);
        assertThat(exception.getResponse()).isNull();

        // and
        assertRequestLogs(request);
    }

    @Test
    public void should_return_NotificationClientException_with_multiple_missing_required_fields_info()
        throws JsonProcessingException {
        // given
        String message = "po_box is required | error_description is required";
        stubWithResponse(getBadRequest(message));
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            "test_zip_file_name",
            null,
            "test_error_code",
            null
        );

        // when
        Throwable throwable = catchThrowable(() -> client.notify(request));

        // then
        assertThrowingResponse(throwable, message);

        // and
        assertRequestLogs(request);
    }

    @Test
    public void should_return_NotificationClientException_with_single_missing_required_field_info()
        throws JsonProcessingException {
        // given
        String message = "zip_file_name is required";
        stubWithResponse(getBadRequest(message));
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            null,
            "test_po_box",
            "test_error_code",
            "test_description"
        );

        // when
        Throwable throwable = catchThrowable(() -> client.notify(request));

        // then
        assertThrowingResponse(throwable, message);

        // and
        assertRequestLogs(request);
    }

    @Test
    public void should_return_NotificationClientException_with_invalid_error_code_info()
        throws JsonProcessingException {
        // given
        String message = "Invalid error code.";
        stubWithResponse(getBadRequest(message));
        ErrorNotificationRequest request = new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            "test_error_code",
            "test_description"
        );

        // when
        Throwable throwable = catchThrowable(() -> client.notify(request));

        // then
        assertThrowingResponse(throwable, message);

        // and
        assertRequestLogs(request);
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
        assertThat(throwable).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException exception = (ErrorNotificationException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo(message);
    }

    private void assertRequestLogs(ErrorNotificationRequest request) {
        List<String> logs = Arrays.asList(capture.toString().split("\n"));
        Pattern pattern = Pattern.compile(
            "INFO.+\\[.+\\] "
                + ErrorNotificationClient.class.getCanonicalName()
                + ":\\d+: Error notification body:"
        );
        boolean existsEntry = logs.stream().anyMatch(entry -> pattern.matcher(entry).find());

        assertThat(existsEntry).isTrue();

        // and
        DocumentContext context = logs
            .stream()
            .filter(entry -> entry.startsWith("{"))
            .map(entry -> {
                try {
                    System.out.println("ENTRY: " + entry);
                    return JsonPath.parse(entry);
                } catch (InvalidJsonException exception) {
                    // just skip it
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        JsonPathAssert pathAssert = new JsonPathAssert(context);

        pathAssert.isNotNull();

        if (request.zipFileName == null) {
            assertThat(context.jsonString()).contains("\"zip_file_name\":null");
        } else {
            pathAssert
                .jsonPathAsString("$.zip_file_name")
                .contains(request.zipFileName);
        }

        if (request.poBox == null) {
            assertThat(context.jsonString()).contains("\"po_box\":null");
        } else {
            pathAssert
                .jsonPathAsString("$.po_box")
                .contains(request.poBox);
        }

        assertThat(context.jsonString()).contains("\"document_control_number\":null");
        pathAssert
            .jsonPathAsString("$.error_code")
            .contains(request.errorCode);

        if (request.errorDescription == null) {
            assertThat(context.jsonString()).contains("\"error_description\":null");
        } else {
            pathAssert
                .jsonPathAsString("$.error_description")
                .contains(request.errorDescription);
        }

        assertThat(context.jsonString()).contains("\"reference_id\":null");
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
