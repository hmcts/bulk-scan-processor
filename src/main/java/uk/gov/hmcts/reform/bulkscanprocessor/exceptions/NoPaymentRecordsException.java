package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class NoPaymentRecordsException extends RuntimeException {

    public NoPaymentRecordsException(String message) {
        super(message);
    }
}
