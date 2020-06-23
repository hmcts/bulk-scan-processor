package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

public class NonPdfFileFoundException extends RejectionException {

    private static final long serialVersionUID = 9143161748679833084L;

    public NonPdfFileFoundException(String zipFileName, String fileName) {
        super(ERR_ZIP_PROCESSING_FAILED, "Zip '" + zipFileName + "' contains non-pdf file: " + fileName);
    }
}
