package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataParseException extends InvalidEnvelopeException {

    public OcrDataParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
