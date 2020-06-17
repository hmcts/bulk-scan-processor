package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import org.springframework.util.CollectionUtils;

import java.util.List;

public class OcrValidationException extends InvalidMetafileException {

    // might contain sensitive data
    private List errors;

    public OcrValidationException(String message) {
        super(message);
    }

    public OcrValidationException(String message, List errors) {
        super(message);
        this.errors = errors;
    }

    public String getDetailMessage() {
        return CollectionUtils.isEmpty(errors)
            ? (super.getMessage() + " Errors : ")
            : (super.getMessage() + " Errors : " + errors);
    }
}
