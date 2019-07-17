package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumberException extends InvalidEnvelopeException {

    public DuplicateDocumentControlNumberException(String message, Throwable cause) {
        super(message, cause);
    }
}
