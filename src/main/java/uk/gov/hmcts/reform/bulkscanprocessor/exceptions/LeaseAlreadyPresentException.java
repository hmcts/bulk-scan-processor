package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class LeaseAlreadyPresentException extends RuntimeException {
    private static final long serialVersionUID = 6136150918143302940L;

    public LeaseAlreadyPresentException(String message) {
        super(message);
    }
}
