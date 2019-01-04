package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Exception representing a generic failure in the process of verifying a signature.
 */
public class SignatureValidationException extends RuntimeException {

    public SignatureValidationException(Throwable t) {
        super(t);
    }

    public SignatureValidationException(String message) {
        super(message);
    }
}
