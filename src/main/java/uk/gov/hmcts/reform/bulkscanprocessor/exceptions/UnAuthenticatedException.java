package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is an unauthenticated exception.
 */
public class UnAuthenticatedException extends RuntimeException {
    private static final long serialVersionUID = -4672282254380424023L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public UnAuthenticatedException(String message) {
        super(message);
    }
}
