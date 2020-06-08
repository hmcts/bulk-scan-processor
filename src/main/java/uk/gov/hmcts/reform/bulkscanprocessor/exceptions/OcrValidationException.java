package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends InvalidMetafileException {
    public OcrValidationException(String message) {
        super(message);
    }
}
