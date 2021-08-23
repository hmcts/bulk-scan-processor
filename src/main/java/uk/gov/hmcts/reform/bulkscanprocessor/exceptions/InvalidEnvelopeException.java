package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Base class for all exceptions that represent invalid envelope situation.
 */
public abstract class InvalidEnvelopeException extends RuntimeException {

    protected InvalidEnvelopeException(String message) {
        super(message);
    }

    protected InvalidEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
