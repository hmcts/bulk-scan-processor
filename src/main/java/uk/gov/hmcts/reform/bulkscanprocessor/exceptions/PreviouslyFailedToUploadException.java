package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PreviouslyFailedToUploadException extends RuntimeException {

    public PreviouslyFailedToUploadException(String message) {
        super(message);
    }
}
