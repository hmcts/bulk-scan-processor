package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationException extends InvalidMetafileException {

    // might contain sensitive data
    private String detailMessage;

    public OcrValidationException(String message) {
        super(message);
        this.detailMessage = message;
    }

    public OcrValidationException(String message, String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
