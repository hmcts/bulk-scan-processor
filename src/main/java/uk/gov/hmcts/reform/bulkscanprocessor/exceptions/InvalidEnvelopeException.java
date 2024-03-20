package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent invalid envelope situation.
 */
public abstract class InvalidEnvelopeException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    protected InvalidEnvelopeException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     * @param cause the cause of the exception
     */
    protected InvalidEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
