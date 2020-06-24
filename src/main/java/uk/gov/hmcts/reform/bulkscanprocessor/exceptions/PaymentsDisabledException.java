package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_PAYMENTS_DISABLED;

public class PaymentsDisabledException extends EnvelopeRejectionException {
    public PaymentsDisabledException(String message) {
        super(ERR_PAYMENTS_DISABLED, message);
    }
}
