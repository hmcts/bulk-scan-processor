package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(Throwable cause) {
        super(cause);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }

}
