package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is an error generating SAS token.
 */
public class UnableToGenerateSasTokenException extends RuntimeException {

    private static final long serialVersionUID = -3484283017479516646L;

    /**
     * Creates a new instance of the exception.
     * @param e the error message
     */
    public UnableToGenerateSasTokenException(Throwable e) {
        super(e);
    }
}
