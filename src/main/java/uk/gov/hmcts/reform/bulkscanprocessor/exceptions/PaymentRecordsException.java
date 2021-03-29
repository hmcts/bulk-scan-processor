package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class PaymentRecordsException extends RuntimeException {

    public PaymentRecordsException(String message) {
        super(message);
    }
}
