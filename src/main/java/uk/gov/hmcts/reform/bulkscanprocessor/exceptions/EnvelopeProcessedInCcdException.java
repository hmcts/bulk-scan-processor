package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeProcessedInCcdException extends EnvelopeStateException {

    public EnvelopeProcessedInCcdException(String message) {
        super(message);
    }
}
