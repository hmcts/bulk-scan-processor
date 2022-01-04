package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class FileSizeExceedMaxUploadLimit extends RuntimeException {

    public FileSizeExceedMaxUploadLimit(String message) {
        super(message);
    }
}
