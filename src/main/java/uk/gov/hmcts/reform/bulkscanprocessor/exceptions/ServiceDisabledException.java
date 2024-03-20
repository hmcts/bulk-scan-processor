package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_SERVICE_DISABLED;

/**
 * An exception to be thrown when service is disabled.
 */
public class ServiceDisabledException extends EnvelopeRejectionException {
    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public ServiceDisabledException(String message) {
        super(ERR_SERVICE_DISABLED, message);
    }
}
