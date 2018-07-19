package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidEnvelopeException extends RuntimeException {

    public InvalidEnvelopeException(String message) {
        super(message);
    }
}
