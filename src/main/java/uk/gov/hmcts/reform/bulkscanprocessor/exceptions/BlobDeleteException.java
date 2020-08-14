package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class BlobDeleteException extends RuntimeException {

    public BlobDeleteException(String message) {
        super(message);
    }
}
