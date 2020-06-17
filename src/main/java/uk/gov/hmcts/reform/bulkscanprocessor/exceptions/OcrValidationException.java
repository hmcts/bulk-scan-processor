package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends InvalidMetafileException {

    // might contain sensitive data
    private final String detailMessage;

    public OcrValidationException(String message) {
        super(message);
        detailMessage = message;
    }

    public OcrValidationException(String message, String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
