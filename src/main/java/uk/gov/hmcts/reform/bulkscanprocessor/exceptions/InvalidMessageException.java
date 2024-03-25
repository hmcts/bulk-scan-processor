package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Thrown when the message is invalid.
 */
public class InvalidMessageException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public InvalidMessageException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     * @param cause the cause of the exception
     */
    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }

}
