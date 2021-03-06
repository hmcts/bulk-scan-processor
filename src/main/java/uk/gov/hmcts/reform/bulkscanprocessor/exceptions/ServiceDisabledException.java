package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_SERVICE_DISABLED;

public class ServiceDisabledException extends EnvelopeRejectionException {
    public ServiceDisabledException(String message) {
        super(ERR_SERVICE_DISABLED, message);
    }
}
