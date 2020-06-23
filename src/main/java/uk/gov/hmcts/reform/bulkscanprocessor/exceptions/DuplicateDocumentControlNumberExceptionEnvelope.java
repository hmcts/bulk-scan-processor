package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

public class DuplicateDocumentControlNumberExceptionEnvelope extends EnvelopeRejectionException {

    public DuplicateDocumentControlNumberExceptionEnvelope(String message, Throwable cause) {
        super(ERR_ZIP_PROCESSING_FAILED, message, cause);
    }
}
