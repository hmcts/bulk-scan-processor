package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent invalid metadata situation.
 */
public abstract class InvalidMetafileException extends RejectionException {

    public InvalidMetafileException(String message) {
        super(message);
    }

    public InvalidMetafileException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMetafileException(String message, String detailMessage) {
        super(message, detailMessage);
    }

}
