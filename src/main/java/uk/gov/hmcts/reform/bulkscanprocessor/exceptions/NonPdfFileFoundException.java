package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

/**
 * An exception to be thrown when a non-pdf file is found in the zip.
 */
public class NonPdfFileFoundException extends EnvelopeRejectionException {

    private static final long serialVersionUID = 9143161748679833084L;

    /**
     * Creates a new instance of the exception.
     * @param zipFileName the name of the zip file
     * @param fileName the name of the non-pdf file
     */
    public NonPdfFileFoundException(String zipFileName, String fileName) {
        super(ERR_ZIP_PROCESSING_FAILED, "Zip '" + zipFileName + "' contains non-pdf file: " + fileName);
    }
}
