package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a zip file load exception.
 */
public class ZipFileLoadException extends RuntimeException {
    /**
     * Creates a new instance of the exception.
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ZipFileLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
