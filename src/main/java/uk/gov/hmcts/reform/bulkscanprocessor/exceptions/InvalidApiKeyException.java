package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
