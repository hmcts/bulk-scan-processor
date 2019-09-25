package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PaymentsDisabledException extends RuntimeException {
    public PaymentsDisabledException(String message) {
        super(message);
    }
}
