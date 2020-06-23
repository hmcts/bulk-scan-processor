package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class OcrValidationExceptionEnvelope extends EnvelopeRejectionException {

    public OcrValidationExceptionEnvelope(String message) {
        super(ERR_METAFILE_INVALID, message);
    }

    public OcrValidationExceptionEnvelope(String message, String detailMessage) {
        super(ERR_METAFILE_INVALID, message, detailMessage);
    }
}
