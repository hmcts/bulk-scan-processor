package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * This exception is thrown when a document control number is found to be duplicate.
 */
public class DuplicateDocumentControlNumbersInEnvelopeException extends
    EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public DuplicateDocumentControlNumbersInEnvelopeException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
