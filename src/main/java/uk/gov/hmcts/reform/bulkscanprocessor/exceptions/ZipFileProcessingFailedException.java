package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent failed zip file processing situation.
 */
public abstract class ZipFileProcessingFailedException extends ProcessorRunTimeException {

    public ZipFileProcessingFailedException(String message) {
        super(message);
    }

    public ZipFileProcessingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
