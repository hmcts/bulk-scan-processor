package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * An exception to be thrown when the name of the zip file does not match the metadata.
 */
public class ZipNameNotMatchingMetaDataException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     * @param message the error message
     */
    public ZipNameNotMatchingMetaDataException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
