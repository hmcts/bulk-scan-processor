package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Indicates that the operation is forbidden.
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
