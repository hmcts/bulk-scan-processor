package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent invalid metadata situation.
 */
public abstract class InvalidMetafileException extends RuntimeException {

    public InvalidMetafileException(String message) {
        super(message);
    }

    public InvalidMetafileException(String message, Throwable cause) {
        super(message, cause);
    }
}
