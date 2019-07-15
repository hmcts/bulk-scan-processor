package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumbersInEnvelopeException extends InvalidEnvelopeException {
    public DuplicateDocumentControlNumbersInEnvelopeException(String message) {
        super(message);
    }
}
