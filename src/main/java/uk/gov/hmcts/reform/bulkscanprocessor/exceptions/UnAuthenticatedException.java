package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class UnAuthenticatedException extends RuntimeException {
    private static final long serialVersionUID = -4672282254380424023L;

    public UnAuthenticatedException(String message) {
        super(message);
    }
}
