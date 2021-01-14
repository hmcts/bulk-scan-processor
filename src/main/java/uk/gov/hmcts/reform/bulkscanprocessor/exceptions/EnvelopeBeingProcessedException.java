package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeBeingProcessedException extends EnvelopeStateException {

    public EnvelopeBeingProcessedException(String message) {
        super(message);
    }
}
