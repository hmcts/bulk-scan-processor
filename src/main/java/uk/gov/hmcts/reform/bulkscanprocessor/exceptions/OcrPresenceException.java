package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * An exception to be thrown when OCR presence is invalid.
 */
public class OcrPresenceException extends EnvelopeRejectionException {
    /**
     * Creates a new instance of the exception.
     *
     * @param message the error message
     */
    public OcrPresenceException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
