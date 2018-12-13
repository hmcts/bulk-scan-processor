package uk.gov.hmcts.reform.bulkscanprocessor.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationFailingResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

class ErrorNotificationDecoder implements ErrorDecoder {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationDecoder.class);

    private static final ErrorDecoder DELEGATE = new ErrorDecoder.Default();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus statusCode = HttpStatus.valueOf(response.status());
        String statusText = Optional.ofNullable(response.reason()).orElse(statusCode.getReasonPhrase());

        byte[] rawBody = new byte[0];
        ErrorNotificationFailingResponse responseBody = null;

        if (response.body() != null && statusCode.is4xxClientError()) {
            try (InputStream body = response.body().asInputStream()) {
                rawBody = IOUtils.toByteArray(body);
                responseBody = MAPPER.readValue(rawBody, ErrorNotificationFailingResponse.class);
            } catch (IOException e) {
                log.error("Failed to process response body.", e);
                // don't fail and let normal exception to be returned
            }
        }

        if (statusCode.is4xxClientError()) {
            HttpClientErrorException clientException = new HttpClientErrorException(
                statusCode,
                statusText,
                rawBody,
                StandardCharsets.UTF_8
            );

            return new NotificationClientException(clientException, responseBody);
        }

        return DELEGATE.decode(methodKey, response);
    }
}
