package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope is not found.
 */
public class EnvelopeNotFoundException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     */
    public EnvelopeNotFoundException() {
        super();
    }

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public EnvelopeNotFoundException(String message) {
        super(message);
    }
}
