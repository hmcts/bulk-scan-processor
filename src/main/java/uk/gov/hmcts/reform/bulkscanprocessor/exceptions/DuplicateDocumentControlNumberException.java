package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumberException extends ZipFileProcessingFailedException {

    public DuplicateDocumentControlNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
