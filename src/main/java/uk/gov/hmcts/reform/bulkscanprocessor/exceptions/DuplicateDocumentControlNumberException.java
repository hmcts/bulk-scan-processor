package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

/**
 * This exception is thrown when a document control number is found to be duplicate.
 */
public class DuplicateDocumentControlNumberException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public DuplicateDocumentControlNumberException(String message, Throwable cause) {
        super(ERR_ZIP_PROCESSING_FAILED, message, cause);
    }
}
