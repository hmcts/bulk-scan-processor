package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_PAYMENTS_DISABLED;

/**
 * An exception to be thrown when payments are disabled.
 */
public class PaymentsDisabledException extends EnvelopeRejectionException {
    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public PaymentsDisabledException(String message) {
        super(ERR_PAYMENTS_DISABLED, message);
    }
}
