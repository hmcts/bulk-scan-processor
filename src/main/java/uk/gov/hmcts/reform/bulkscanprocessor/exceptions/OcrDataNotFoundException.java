package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataNotFoundException extends InvalidMetafileException {

    public OcrDataNotFoundException(String message) {
        super(message);
    }
}
