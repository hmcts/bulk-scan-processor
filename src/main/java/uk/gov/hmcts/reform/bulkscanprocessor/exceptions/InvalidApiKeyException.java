package uk.gov.hmcts.reform.blobrouter.exceptions;

public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
