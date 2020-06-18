package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

@SuppressWarnings("squid:S110") // todo tidy up exceptions
public class OcrValidationException extends InvalidMetafileException {

    public OcrValidationException(String message) {
        super(message);
    }

    public OcrValidationException(String message, String detailMessage) {
        super(message, detailMessage);
    }
}
