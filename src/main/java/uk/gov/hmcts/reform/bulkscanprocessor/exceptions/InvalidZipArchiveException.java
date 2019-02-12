package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class InvalidZipArchiveException extends InvalidEnvelopeException {
    public InvalidZipArchiveException(String message) {
        super(message);
    }

    public InvalidZipArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
