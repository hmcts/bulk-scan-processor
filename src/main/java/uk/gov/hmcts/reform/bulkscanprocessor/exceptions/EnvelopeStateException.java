package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for exceptions that indicate that the envelope is in an invalid state.
 */
public abstract class EnvelopeStateException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     */
    protected EnvelopeStateException(String message) {
        super(message);
    }
}
