package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

public class ZipNameNotMatchingMetaDataException extends RejectionException {

    public ZipNameNotMatchingMetaDataException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
