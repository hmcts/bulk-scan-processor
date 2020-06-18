package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class FileNameIrregularitiesException extends RejectionException implements InvalidMetafileException {

    public FileNameIrregularitiesException(String message) {
        super(message);
    }
}
