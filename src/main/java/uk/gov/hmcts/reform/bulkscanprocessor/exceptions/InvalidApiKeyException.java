package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Indicates that the API key is invalid.
 */
public class InvalidApiKeyException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
