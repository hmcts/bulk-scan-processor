package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public abstract class EnvelopeStateException extends RuntimeException {

    protected EnvelopeStateException(String message) {
        super(message);
    }
}
