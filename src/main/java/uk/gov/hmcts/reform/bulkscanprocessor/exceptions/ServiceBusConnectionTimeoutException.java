package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ServiceBusConnectionTimeoutException extends RuntimeException {

    public ServiceBusConnectionTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
