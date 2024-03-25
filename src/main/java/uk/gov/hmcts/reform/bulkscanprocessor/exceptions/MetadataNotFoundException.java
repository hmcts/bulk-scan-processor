package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

/**
 * An exception to be thrown when metadata is not found in the zip file.
 */
public class MetadataNotFoundException extends EnvelopeRejectionException {
    private static final long serialVersionUID = 1395897001657755898L;

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public MetadataNotFoundException(String message) {
        super(ERR_ZIP_PROCESSING_FAILED, message);
    }
}
