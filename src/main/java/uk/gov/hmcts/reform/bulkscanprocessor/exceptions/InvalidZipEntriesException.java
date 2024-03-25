package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

/**
 * An exception to be thrown when there is a configuration error.
 */
public class InvalidZipEntriesException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public InvalidZipEntriesException(String message) {
        super(ERR_ZIP_PROCESSING_FAILED, message);
    }
}
