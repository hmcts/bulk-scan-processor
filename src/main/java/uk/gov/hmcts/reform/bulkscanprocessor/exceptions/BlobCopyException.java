package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class BlobCopyException extends RuntimeException {

    public BlobCopyException(String message, Throwable cause) {
        super(message, cause);
    }
}
