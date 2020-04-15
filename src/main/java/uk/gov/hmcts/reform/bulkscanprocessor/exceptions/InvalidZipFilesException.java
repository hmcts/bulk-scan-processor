package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidZipFilesException extends RuntimeException {
    public InvalidZipFilesException(String message) {
        super(message);
    }
}
