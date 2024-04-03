package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Indicates that the file size exceeds the maximum upload limit.
 */
public class FileSizeExceedMaxUploadLimit extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public FileSizeExceedMaxUploadLimit(String message) {
        super(message);
    }
}
