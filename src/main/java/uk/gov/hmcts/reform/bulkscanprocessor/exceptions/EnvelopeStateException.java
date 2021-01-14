package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public abstract class EnvelopeStateException extends RuntimeException {

    public EnvelopeStateException(String message) {
        super(message);
    }
}
