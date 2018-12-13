package uk.gov.hmcts.reform.bulkscanprocessor.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationFailingResponse;

public class NotificationClientException extends RuntimeException {

    private final HttpStatus status;

    private final transient ErrorNotificationFailingResponse response;

    NotificationClientException(HttpClientErrorException cause, ErrorNotificationFailingResponse response) {
        super(cause);

        this.status = cause.getStatusCode();
        this.response = response;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorNotificationFailingResponse getResponse() {
        return response;
    }
}
