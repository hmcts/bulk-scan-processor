package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

}
