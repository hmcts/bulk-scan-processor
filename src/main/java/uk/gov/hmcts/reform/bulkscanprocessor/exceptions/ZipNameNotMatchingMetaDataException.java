package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ZipNameNotMatchingMetaDataException extends RejectionException implements InvalidMetafileException {

    public ZipNameNotMatchingMetaDataException(String message) {
        super(message);
    }
}
