package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PaymentsDisabledException extends ProcessorRunTimeException {
    public PaymentsDisabledException(String message) {
        super(message);
    }
}
