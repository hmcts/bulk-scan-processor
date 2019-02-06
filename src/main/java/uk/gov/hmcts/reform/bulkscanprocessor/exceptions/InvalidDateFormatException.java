package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidDateFormatException extends RuntimeException {

    public InvalidDateFormatException(String format) {
        this(format, null);
    }

    public InvalidDateFormatException(String format, Throwable cause) {
        super(format, cause);
    }
}
