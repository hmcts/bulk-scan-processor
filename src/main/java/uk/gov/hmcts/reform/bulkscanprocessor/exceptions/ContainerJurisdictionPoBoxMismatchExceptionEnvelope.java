package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class ContainerJurisdictionPoBoxMismatchExceptionEnvelope extends
    EnvelopeRejectionException {
    public ContainerJurisdictionPoBoxMismatchExceptionEnvelope(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
