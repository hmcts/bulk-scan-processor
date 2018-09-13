package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ConnectionException extends RuntimeException {

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
