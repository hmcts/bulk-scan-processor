package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ServiceDisabledException extends RejectionException {
    public ServiceDisabledException(String message) {
        super(message);
    }
}
