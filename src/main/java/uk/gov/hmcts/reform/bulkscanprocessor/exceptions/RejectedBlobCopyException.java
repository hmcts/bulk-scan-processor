package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a rejected blob copy exception.
 */
public class RejectedBlobCopyException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public RejectedBlobCopyException(String message) {
        super(message);
    }
}
