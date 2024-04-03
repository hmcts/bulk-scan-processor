package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

/**
 * An exception to be thrown when there is a problem with payment records.
 */
public class PaymentRecordsException extends RuntimeException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the error message
     */
    public PaymentRecordsException(String message) {
        super(message);
    }
}
