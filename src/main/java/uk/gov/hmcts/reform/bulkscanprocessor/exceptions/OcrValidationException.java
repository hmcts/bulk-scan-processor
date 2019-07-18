package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends InvalidEnvelopeException {
    public OcrValidationException(String message) {
        super(message);
    }
}
