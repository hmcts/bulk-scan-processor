package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
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
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureWireMock
@IntegrationTest
@RunWith(SpringRunner.class)
public class ErrorNotificationClientTest {

    @Autowired
    private ErrorNotificationClient client;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void should_return_Created_when_everything_is_ok_with_request() throws JsonProcessingException {
        // given
        ErrorNotificationResponse response = new ErrorNotificationResponse("some id");
        stubWithResponse(created().withBody(mapper.writeValueAsBytes(response)));

        // when
        ErrorNotificationResponse notificationResponse = client.notify(new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            ERR_METAFILE_INVALID.name(),
            "test_error_description",
            "test_service"
        ));

        // then
        assertThat(notificationResponse).isEqualToComparingFieldByField(response);
    }

    @Test
    public void should_return_NotificationClientException_when_badly_authorised() {
        // given
        stubWithResponse(unauthorized());

        // when
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            ERR_METAFILE_INVALID.name(),
            "test_error_description",
            "test_service"
        )));

        // then
        assertThat(throwable).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException exception = (ErrorNotificationException) throwable;

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
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest(
            "test_zip_file_name",
            null,
            "test_error_code",
            null,
            "test_service"
        )));

        // then
        assertThrowingResponse(throwable, message);
    }

    @Test
    public void should_return_NotificationClientException_with_single_missing_required_field_info()
        throws JsonProcessingException {
        // given
        String message = "zip_file_name is required";
        stubWithResponse(getBadRequest(message));

        // when
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest(
            null,
            "test_po_box",
            "test_error_code",
            "test_description",
            "test_service"
        )));

        // then
        assertThrowingResponse(throwable, message);
    }

    @Test
    public void should_return_NotificationClientException_with_invalid_error_code_info()
        throws JsonProcessingException {
        // given
        String message = "Invalid error code.";
        stubWithResponse(getBadRequest(message));

        // when
        Throwable throwable = catchThrowable(() -> client.notify(new ErrorNotificationRequest(
            "test_zip_file_name",
            "test_po_box",
            "test_error_code",
            "test_description",
            "test_service"
        )));

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
        assertThat(throwable).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException exception = (ErrorNotificationException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo(message);
    }
}
