package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeNotCompletedOrStaleException extends EnvelopeStateException {

    public EnvelopeNotCompletedOrStaleException(String message) {
        super(message);
    }
}
