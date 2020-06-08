package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ZipNameNotMatchingMetaDataException extends InvalidMetafileException {

    public ZipNameNotMatchingMetaDataException(String message) {
        super(message);
    }
}
