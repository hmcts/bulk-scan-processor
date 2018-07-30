package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidStatusChangeException extends RuntimeException {

    public InvalidStatusChangeException(String message) {
        super(message);
    }
}
