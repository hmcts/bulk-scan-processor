package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

/**
 * This exception is thrown when envelope is rejected.
 */
public abstract class EnvelopeRejectionException extends RuntimeException {

    // might contain sensitive data
    private final String errorDescription;
    private final ErrorCode errorCode;

    /**
     * Creates a new instance of the exception.
     * @param errorCode the error code
     * @param message the exception message
     */
    protected EnvelopeRejectionException(ErrorCode errorCode, String message) {
        super(message);
        //use message as error description
        this.errorDescription = message;
        this.errorCode = errorCode;
    }

    /**
     * Creates a new instance of the exception.
     * @param errorCode the error code
     * @param message the exception message
     * @param cause the cause of the exception
     */
    protected EnvelopeRejectionException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        //use message as error description
        this.errorDescription = message;
        this.errorCode = errorCode;
    }

    /**
     * Creates a new instance of the exception.
     * @param errorCode the error code
     * @param message the exception message
     * @param errorDescription the error description
     */
    protected EnvelopeRejectionException(ErrorCode errorCode, String message, String errorDescription) {
        super(message);
        this.errorDescription = errorDescription;
        this.errorCode = errorCode;
    }

    /**
     * Get the error description.
     * @return the error description
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Get the error code.
     * @return the error code
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
