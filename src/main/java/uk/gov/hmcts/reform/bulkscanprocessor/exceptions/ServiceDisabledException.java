package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ServiceDisabledException extends ProcessorRunTimeException {
    public ServiceDisabledException(String message) {
        super(message);
    }
}
