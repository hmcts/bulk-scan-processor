package uk.gov.hmcts.reform.bulkscanprocessor.config;

public class FailedToRegisterMessageHandlersExcepiton extends RuntimeException {

    public FailedToRegisterMessageHandlersExcepiton(String message, Throwable cause) {
        super(message, cause);
    }
}
