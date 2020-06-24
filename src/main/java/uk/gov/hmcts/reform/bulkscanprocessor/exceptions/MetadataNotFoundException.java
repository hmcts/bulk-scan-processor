package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

public class MetadataNotFoundException extends EnvelopeRejectionException {
    private static final long serialVersionUID = 1395897001657755898L;

    public MetadataNotFoundException(String message) {
        super(ERR_ZIP_PROCESSING_FAILED, message);
    }
}
