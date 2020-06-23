package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

public abstract class RejectionException extends RuntimeException {

    // might contain sensitive data
    private final String detailMessage;
    private final ErrorCode errorCode;

    public RejectionException(ErrorCode errorCode, String message) {
        super(message);
        this.detailMessage = message;
        this.errorCode = errorCode;
    }

    public RejectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.detailMessage = message;
        this.errorCode = errorCode;
    }

    public RejectionException(ErrorCode errorCode, String message, String detailMessage) {
        super(message);
        this.detailMessage = detailMessage;
        this.errorCode = errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
