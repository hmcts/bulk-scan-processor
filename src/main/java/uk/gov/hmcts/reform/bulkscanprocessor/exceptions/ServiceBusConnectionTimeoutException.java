package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a service bus connection timeout exception.
 */
public class ServiceBusConnectionTimeoutException extends RuntimeException {

    private static final long serialVersionUID = -1850692854300268468L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ServiceBusConnectionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
