package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ContainerJurisdictionPoBoxMismatchException extends RejectionException
    implements InvalidMetafileException {

    public ContainerJurisdictionPoBoxMismatchException(String message) {
        super(message);
    }
}
