package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidZipFilesException extends InvalidEnvelopeException {
    public InvalidZipFilesException(String message) {
        super(message);
    }
}
