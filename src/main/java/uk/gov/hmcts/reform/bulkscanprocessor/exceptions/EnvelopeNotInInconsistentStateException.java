package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope is not in inconsistent state.
 */
public class EnvelopeNotInInconsistentStateException extends EnvelopeStateException {

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     */
    public EnvelopeNotInInconsistentStateException(String message) {
        super(message);
    }
}
