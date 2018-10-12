package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class SignatureValidationException extends RuntimeException {

    public SignatureValidationException(Throwable t) {
        super(t);
    }

    public SignatureValidationException(String message) {
        super(message);
    }
}
