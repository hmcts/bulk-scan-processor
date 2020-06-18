package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumbersInEnvelopeException extends InvalidMetafileException {
    public DuplicateDocumentControlNumbersInEnvelopeException(String message) {
        super(message);
    }
}
