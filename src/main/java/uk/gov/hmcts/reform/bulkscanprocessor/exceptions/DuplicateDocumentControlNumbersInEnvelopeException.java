package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumbersInEnvelopeException extends RuntimeException {
    public DuplicateDocumentControlNumbersInEnvelopeException(String message) {
        super(message);
    }
}
