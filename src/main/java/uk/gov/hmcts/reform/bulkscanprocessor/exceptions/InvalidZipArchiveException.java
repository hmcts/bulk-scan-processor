package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a configuration error.
 */
public class InvalidZipArchiveException extends InvalidEnvelopeException {
    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public InvalidZipArchiveException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     * @param cause the cause of the exception
     */
    public InvalidZipArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
