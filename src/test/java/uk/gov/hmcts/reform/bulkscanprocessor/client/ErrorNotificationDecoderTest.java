package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorNotificationDecoderTest {

    private static final ErrorNotificationDecoder DECODER = new ErrorNotificationDecoder(new ObjectMapper());

    private static final String METHOD_KEY = "key";

    private static final Request REQUEST = Request.create(
        Request.HttpMethod.GET,
        "/",
        Collections.emptyMap(),
        null
    );

    @Test
    public void should_return_ErrorNotificationException_when_response_is_4xx_and_unknown_body() {
        // given
        Response response = Response.builder()
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
            .headers(Collections.emptyMap())
            .request(REQUEST)
            .body("Unsupported media type".getBytes())
            .build();

        // when
        Exception exception = DECODER.decode(METHOD_KEY, response);

        // then
        assertThat(exception).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException errorException = (ErrorNotificationException) exception;

        assertThat(errorException.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(errorException.getResponse()).isNull();
        assertThat(errorException.getCause()).isInstanceOf(HttpClientErrorException.class);
        assertThat(((HttpClientErrorException) errorException.getCause()).getResponseBodyAsString())
            .isEqualTo("Unsupported media type");
    }

    @Test
    public void should_return_ErrorNotificationException_when_response_is_5xx() {
        // given
        Response response = Response.builder()
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .headers(Collections.emptyMap())
            .request(REQUEST)
            .body("Service unavailable".getBytes())
            .build();

        // when
        Exception exception = DECODER.decode(METHOD_KEY, response);

        // then
        assertThat(exception).isInstanceOf(ErrorNotificationException.class);

        // and
        ErrorNotificationException errorException = (ErrorNotificationException) exception;

        assertThat(errorException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(errorException.getCause()).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    public void should_return_FeignException_when_response_is_unknown() {
        // given
        Response response = Response.builder()
            .status(HttpStatus.PERMANENT_REDIRECT.value())
            .headers(Collections.emptyMap())
            .request(REQUEST)
            .body("Did not want to do that".getBytes())
            .build();

        // when
        Exception exception = DECODER.decode(METHOD_KEY, response);

        // then
        assertThat(exception).isInstanceOf(FeignException.class);
        assertThat(((FeignException) exception).status()).isEqualTo(HttpStatus.PERMANENT_REDIRECT.value());
    }
}
