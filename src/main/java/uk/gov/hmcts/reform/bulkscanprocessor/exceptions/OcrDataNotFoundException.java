package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataNotFoundException extends RuntimeException {

    public OcrDataNotFoundException(String message) {
        super(message);
    }
}
