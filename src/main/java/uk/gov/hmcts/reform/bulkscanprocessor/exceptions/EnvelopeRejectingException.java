package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope is processed in CCD.
 */
public class EnvelopeRejectingException extends RuntimeException {
    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     */
    public EnvelopeRejectingException(String message, Throwable cause) {
        super(message, cause);
    }
}
