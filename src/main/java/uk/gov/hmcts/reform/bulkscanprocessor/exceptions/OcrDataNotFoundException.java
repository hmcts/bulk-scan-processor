package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * An exception to be thrown when OCR data is not found in the envelope.
 */
public class OcrDataNotFoundException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public OcrDataNotFoundException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
