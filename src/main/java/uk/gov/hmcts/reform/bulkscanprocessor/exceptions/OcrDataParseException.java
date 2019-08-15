package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

public class OcrDataParseException extends JsonMappingException {

    public OcrDataParseException(JsonParser jsonParser, String message, Throwable cause) {
        super(jsonParser, message, cause);
    }
}
