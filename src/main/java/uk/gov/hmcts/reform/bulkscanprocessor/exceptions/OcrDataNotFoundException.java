package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataNotFoundException extends InvalidEnvelopeException {

    public OcrDataNotFoundException(String message) {
        super(message);
    }
}
