package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

public class OcrValidationServerSideException extends RuntimeException {

    public OcrValidationServerSideException(String msg) {
        super(msg);
    }
}
