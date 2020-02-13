package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class ZipNameNotMatchingMetaDataException extends InvalidEnvelopeException {

    public ZipNameNotMatchingMetaDataException(String message) {
        super(message);
    }
}
