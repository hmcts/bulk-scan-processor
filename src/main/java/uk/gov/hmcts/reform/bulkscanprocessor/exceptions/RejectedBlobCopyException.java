package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class RejectedBlobCopyException extends RuntimeException {

    public RejectedBlobCopyException(String message) {
        super(message);
    }
}
