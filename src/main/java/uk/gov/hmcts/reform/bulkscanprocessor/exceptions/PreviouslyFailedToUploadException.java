package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PreviouslyFailedToUploadException extends Exception {

    public PreviouslyFailedToUploadException(String message) {
        super(message);
    }
}
