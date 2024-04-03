package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

/**
 * Indicates that the file name is not as expected.
 */
public class FileNameIrregularitiesException extends EnvelopeRejectionException {

    /**
     * Creates a new instance of the exception.
     *
     * @param message the exception message
     */
    public FileNameIrregularitiesException(String message) {
        super(ERR_METAFILE_INVALID, message);
    }
}
