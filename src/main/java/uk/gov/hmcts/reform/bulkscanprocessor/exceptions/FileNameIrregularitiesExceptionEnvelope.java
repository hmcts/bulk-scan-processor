package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class FileNameIrregularitiesExceptionEnvelope extends EnvelopeRejectionException {

    public FileNameIrregularitiesExceptionEnvelope(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
