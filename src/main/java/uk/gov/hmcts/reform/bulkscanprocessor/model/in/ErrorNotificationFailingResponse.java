package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorNotificationFailingResponse {

    private final String message;

    public ErrorNotificationFailingResponse(@JsonProperty("Message") String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
