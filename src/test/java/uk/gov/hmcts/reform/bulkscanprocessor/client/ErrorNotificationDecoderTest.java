package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Response;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorNotificationDecoderTest {

    private static final ErrorNotificationDecoder DECODER = new ErrorNotificationDecoder(new ObjectMapper());

    private static final String METHOD_KEY = "key";

    @Test
    public void should_return_NotificationClientException_when_response_is_4xx_and_unknown_body() {
        // given
        Response response = Response.builder()
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
            .headers(Collections.emptyMap())
            .body("Unsupported media type".getBytes())
            .build();

        // when
        Exception exception = DECODER.decode(METHOD_KEY, response);

        // then
        assertThat(exception).isInstanceOf(NotificationClientException.class);

        // and
        NotificationClientException clientException = (NotificationClientException) exception;

        assertThat(clientException.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(clientException.getResponse()).isNull();
        assertThat(clientException.getCause()).isInstanceOf(HttpClientErrorException.class);
        assertThat(((HttpClientErrorException) clientException.getCause()).getResponseBodyAsString())
            .isEqualTo("Unsupported media type");
    }

    @Test
    public void should_return_FeignException_when_response_is_5xx() {
        // given
        Response response = Response.builder()
            .status(HttpStatus.SERVICE_UNAVAILABLE.value())
            .headers(Collections.emptyMap())
            .body("Service unavailable".getBytes())
            .build();

        // when
        Exception exception = DECODER.decode(METHOD_KEY, response);

        // then
        assertThat(exception).isInstanceOf(FeignException.class);
        assertThat(((FeignException) exception).status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
