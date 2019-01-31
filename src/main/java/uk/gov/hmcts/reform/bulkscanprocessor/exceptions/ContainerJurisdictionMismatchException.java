package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ContainerJurisdictionMismatchException extends InvalidEnvelopeException {
    public ContainerJurisdictionMismatchException(String message) {
        super(message);
    }
}
