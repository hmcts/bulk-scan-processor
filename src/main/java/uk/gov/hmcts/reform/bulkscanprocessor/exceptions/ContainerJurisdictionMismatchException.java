package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ContainerJurisdictionMismatchException extends RuntimeException {
    public ContainerJurisdictionMismatchException(String message) {
        super(message);
    }
}
