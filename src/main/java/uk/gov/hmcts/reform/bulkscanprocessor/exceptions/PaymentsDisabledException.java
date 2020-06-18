package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PaymentsDisabledException extends RejectionException {
    public PaymentsDisabledException(String message) {
        super(message);
    }
}
