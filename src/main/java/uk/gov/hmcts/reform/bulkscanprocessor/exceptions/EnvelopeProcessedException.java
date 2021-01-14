package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeProcessedException extends EnvelopeStateException {

    public EnvelopeProcessedException(String message) {
        super(message);
    }
}
