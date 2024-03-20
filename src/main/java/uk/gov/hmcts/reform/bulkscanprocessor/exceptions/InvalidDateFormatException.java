package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Indicates that the date format is invalid.
 */
public class InvalidDateFormatException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param format the invalid date format
     */
    public InvalidDateFormatException(String format) {
        this(format, null);
    }

    /**
     * Creates a new instance of the exception.
     *
     * @param format the invalid date format
     * @param cause the cause of the exception
     */
    public InvalidDateFormatException(String format, Throwable cause) {
        super(format, cause);
    }
}
