package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent invalid envelope situation.
 */
public abstract class InvalidEnvelopeException extends RuntimeException {

    public InvalidEnvelopeException(String message) {
        super(message);
    }

    public InvalidEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
