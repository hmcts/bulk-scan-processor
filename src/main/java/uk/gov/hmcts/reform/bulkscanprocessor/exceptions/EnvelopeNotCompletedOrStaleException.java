package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope is not in a state to be completed or is stale.
 */
public class EnvelopeNotCompletedOrStaleException extends EnvelopeStateException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public EnvelopeNotCompletedOrStaleException(String message) {
        super(message);
    }
}
