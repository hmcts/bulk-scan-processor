package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class IncompleteResponseException extends RuntimeException {

    public IncompleteResponseException(String message) {
        super(message);
    }
}
