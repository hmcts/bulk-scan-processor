package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when a blob cannot be deleted.
 */
public class BlobDeleteException extends RuntimeException {

    /**
     * Creates a new instance of the blob delete exception.
     * @param message the exception message
     */
    public BlobDeleteException(String message) {
        super(message);
    }
}
