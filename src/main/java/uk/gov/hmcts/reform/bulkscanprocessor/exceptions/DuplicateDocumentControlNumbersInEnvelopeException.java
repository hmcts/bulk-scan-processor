package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DuplicateDocumentControlNumbersInEnvelopeException extends RejectionException
    implements InvalidMetafileException {

    public DuplicateDocumentControlNumbersInEnvelopeException(String message) {
        super(message);
    }
}
