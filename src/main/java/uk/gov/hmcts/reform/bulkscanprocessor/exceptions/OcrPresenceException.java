package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrPresenceException extends InvalidEnvelopeException {
    public OcrPresenceException(String message) {
        super(message);
    }
}
