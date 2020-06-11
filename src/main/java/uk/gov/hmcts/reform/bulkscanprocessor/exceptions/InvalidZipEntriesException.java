package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidZipEntriesException extends ZipFileProcessingFailedException {
    public InvalidZipEntriesException(String message) {
        super(message);
    }
}
