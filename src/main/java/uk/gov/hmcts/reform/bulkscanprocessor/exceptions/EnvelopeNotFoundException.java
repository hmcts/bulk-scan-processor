package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeNotFoundException extends RuntimeException {

    public EnvelopeNotFoundException() {
        super();
    }

    public EnvelopeNotFoundException(String message) {
        super(message);
    }
}
