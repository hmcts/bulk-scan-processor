package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * An exception to be thrown when the document type is not allowed.
 */
public class DisallowedDocumentTypesException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     * @param message the exception message
     */
    public DisallowedDocumentTypesException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
