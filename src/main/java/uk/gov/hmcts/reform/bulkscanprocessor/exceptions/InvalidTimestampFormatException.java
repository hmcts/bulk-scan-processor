package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidTimestampFormatException extends RuntimeException {

    public InvalidTimestampFormatException(String format) {
        this(format, null);
    }

    public InvalidTimestampFormatException(String format, Throwable cause) {
        super(format, cause);
    }
}
