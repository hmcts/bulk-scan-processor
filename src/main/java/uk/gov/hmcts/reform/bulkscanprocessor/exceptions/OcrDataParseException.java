package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrDataParseException extends RuntimeException {

    public OcrDataParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
