package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidEnvelopeSchemaException extends RuntimeException {

    public InvalidEnvelopeSchemaException(String message) {
        super(message);
    }
}
