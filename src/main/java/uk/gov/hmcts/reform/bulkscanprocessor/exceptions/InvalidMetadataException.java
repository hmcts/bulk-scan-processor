package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidMetadataException extends RuntimeException {

    public InvalidMetadataException(String message) {
        super(message);
    }
}
