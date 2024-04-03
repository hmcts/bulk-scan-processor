package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * This exception is thrown when envelope is processed in CCD.
 */
public class EnvelopeProcessedInCcdException extends EnvelopeStateException {

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     */
    public EnvelopeProcessedInCcdException(String message) {
        super(message);
    }
}
