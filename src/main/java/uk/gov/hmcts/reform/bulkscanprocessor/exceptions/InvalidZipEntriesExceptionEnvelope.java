package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

public class InvalidZipEntriesExceptionEnvelope extends EnvelopeRejectionException {

    public InvalidZipEntriesExceptionEnvelope(String message) {
        super(ERR_ZIP_PROCESSING_FAILED, message);
    }
}
