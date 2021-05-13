package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class EnvelopeNotInInconsistentStateException extends EnvelopeStateException {

    public EnvelopeNotInInconsistentStateException(String message) {
        super(message);
    }
}
