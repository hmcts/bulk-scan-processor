package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is an error while uploading a document.
 */
public class UnableToUploadDocumentException extends RuntimeException {
    private static final long serialVersionUID = -7565272316796306939L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     * @param throwable the cause of the exception
     */
    public UnableToUploadDocumentException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
