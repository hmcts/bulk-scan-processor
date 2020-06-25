package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class FileNameIrregularitiesException extends EnvelopeRejectionException {

    public FileNameIrregularitiesException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
