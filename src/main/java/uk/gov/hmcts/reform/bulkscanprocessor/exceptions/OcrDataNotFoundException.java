package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataNotFoundException extends RejectionException implements InvalidMetafileException {

    public OcrDataNotFoundException(String message) {
        super(message);
    }
}
