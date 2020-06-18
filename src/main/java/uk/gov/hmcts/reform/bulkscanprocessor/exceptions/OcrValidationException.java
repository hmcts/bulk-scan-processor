package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends RejectionException implements InvalidMetafileException {

    public OcrValidationException(String message) {
        super(message);
    }

    public OcrValidationException(String message, String detailMessage) {
        super(message, detailMessage);
    }
}
