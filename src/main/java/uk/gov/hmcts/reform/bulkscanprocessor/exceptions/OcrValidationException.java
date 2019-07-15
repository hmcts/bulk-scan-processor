package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends RuntimeException {
    public OcrValidationException(String message) {
        super(message);
    }
}
