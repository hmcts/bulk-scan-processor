package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a previously failed to upload exception.
 */
public class PreviouslyFailedToUploadException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public PreviouslyFailedToUploadException(String message) {
        super(message);
    }
}
