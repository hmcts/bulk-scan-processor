package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidZipEntriesException extends InvalidEnvelopeException {
    public InvalidZipEntriesException(String message) {
        super(message);
    }
}
