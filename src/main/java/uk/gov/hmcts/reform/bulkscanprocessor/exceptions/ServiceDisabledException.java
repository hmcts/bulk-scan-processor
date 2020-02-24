package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ServiceDisabledException extends RuntimeException {
    public ServiceDisabledException(String message) {
        super(message);
    }
}
