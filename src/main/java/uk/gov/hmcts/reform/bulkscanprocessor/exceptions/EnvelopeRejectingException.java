package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeRejectingException extends RuntimeException {
    public EnvelopeRejectingException(String message, Throwable cause) {
        super(message, cause);
    }
}
