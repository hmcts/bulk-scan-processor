package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DisallowedDocumentTypesException extends InvalidEnvelopeException {

    public DisallowedDocumentTypesException(String message) {
        super(message);
    }
}
