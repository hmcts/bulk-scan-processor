package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DocSignatureFailureException extends RuntimeException {

    public DocSignatureFailureException(String message) {
        super(message);
    }
}
