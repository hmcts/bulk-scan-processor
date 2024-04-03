package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * An exception to be thrown when OCR validation fails.
 */
public class OcrValidationException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the error message
     */
    public OcrValidationException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }

    /**
     * Creates a new instance of the exception.
     *
     * @param message the error message
     * @param detailMessage the detail message
     */
    public OcrValidationException(String message, String detailMessage) {
        super(ERR_METAFILE_INVALID, message, detailMessage);
    }
}
