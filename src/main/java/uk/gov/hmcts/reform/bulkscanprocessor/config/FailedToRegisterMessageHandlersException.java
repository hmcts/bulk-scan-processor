package uk.gov.hmcts.reform.bulkscanprocessor.config;

/**
 * An exception to be thrown when the application fails to register message handlers.
 */
public class FailedToRegisterMessageHandlersException extends RuntimeException {

    /**
     * Creates an instance of this exception with the given message.
     */
    public FailedToRegisterMessageHandlersException(String message, Throwable cause) {
        super(message, cause);
    }
}
