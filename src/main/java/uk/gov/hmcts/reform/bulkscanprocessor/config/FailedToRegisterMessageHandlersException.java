package uk.gov.hmcts.reform.bulkscanprocessor.config;

public class FailedToRegisterMessageHandlersException extends RuntimeException {

    public FailedToRegisterMessageHandlersException(String message, Throwable cause) {
        super(message, cause);
    }
}
