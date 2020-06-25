package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class OcrValidationException extends EnvelopeRejectionException {

    public OcrValidationException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }

    public OcrValidationException(String message, String detailMessage) {
        super(ERR_METAFILE_INVALID, message, detailMessage);
    }
}
