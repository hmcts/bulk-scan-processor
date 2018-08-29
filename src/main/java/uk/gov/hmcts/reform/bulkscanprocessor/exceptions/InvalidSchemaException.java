package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidSchemaException extends RuntimeException {

    public InvalidSchemaException(String message) {
        super(message);
    }
}
