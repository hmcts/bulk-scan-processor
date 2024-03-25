package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope classification fails.
 */
public class EnvelopeClassificationException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public EnvelopeClassificationException(String message) {
        super(message);
    }
}
