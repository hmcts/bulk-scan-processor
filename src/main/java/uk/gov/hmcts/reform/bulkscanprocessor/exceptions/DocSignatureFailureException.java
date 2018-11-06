package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class DocSignatureFailureException extends Exception {

    public DocSignatureFailureException(String message) {
        super(message);
    }
}
