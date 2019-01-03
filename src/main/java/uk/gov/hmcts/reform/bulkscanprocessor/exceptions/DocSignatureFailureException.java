package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * Exception representing a failure because of an invalid signature.
 */
public class DocSignatureFailureException extends RuntimeException {

    public DocSignatureFailureException(String message) {
        super(message);
    }

    public DocSignatureFailureException(String message, Exception cause) {
        super(message, cause);
    }
}
