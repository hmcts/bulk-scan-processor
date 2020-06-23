package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_PAYMENTS_DISABLED;

public class PaymentsDisabledExceptionEnvelope extends EnvelopeRejectionException {
    public PaymentsDisabledExceptionEnvelope(String message) {
        super(ERR_PAYMENTS_DISABLED, message);
    }
}
