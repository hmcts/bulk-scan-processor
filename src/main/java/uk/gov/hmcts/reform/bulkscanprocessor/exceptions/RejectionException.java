package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public abstract class RejectionException extends RuntimeException {

    // might contain sensitive data
    private final String detailMessage;

    public RejectionException(String message) {
        super(message);
        detailMessage = message;

    }

    public RejectionException(String message, Throwable cause) {
        super(message, cause);
        detailMessage = message;
    }


    public RejectionException(String message, String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }
}
