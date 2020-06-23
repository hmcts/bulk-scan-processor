package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

public abstract class EnvelopeRejectionException extends RuntimeException {

    // might contain sensitive data
    private final String errorDescription;
    private final ErrorCode errorCode;

    public EnvelopeRejectionException(ErrorCode errorCode, String message) {
        super(message);
        this.errorDescription = message;
        this.errorCode = errorCode;
    }

    public EnvelopeRejectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        //use message as error description
        this.errorDescription = message;
        this.errorCode = errorCode;
    }

    public EnvelopeRejectionException(ErrorCode errorCode, String message, String errorDescription) {
        super(message);
        this.errorDescription = errorDescription;
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
