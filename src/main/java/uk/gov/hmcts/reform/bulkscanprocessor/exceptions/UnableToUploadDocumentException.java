package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class UnableToUploadDocumentException extends RuntimeException {
    private static final long serialVersionUID = -7565272316796306939L;

    public UnableToUploadDocumentException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
