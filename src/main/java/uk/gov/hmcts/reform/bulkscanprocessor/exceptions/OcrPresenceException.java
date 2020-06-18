package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrPresenceException extends RejectionException implements InvalidMetafileException {
    public OcrPresenceException(String message) {
        super(message);
    }
}
