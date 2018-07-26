package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
