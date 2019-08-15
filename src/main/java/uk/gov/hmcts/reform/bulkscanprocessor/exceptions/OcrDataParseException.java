package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Represents a situation where OCR data format is invalid.
 *
 * <p>
 * Needs to extend JsonMappingException, so that we can catch it - otherwise
 * Jackson would wrap it in its own exception when parsing metadata file.
 * </p>
 */
public class OcrDataParseException extends JsonMappingException {

    public OcrDataParseException(JsonParser jsonParser, String message, Throwable cause) {
        super(jsonParser, message, cause);
    }
}
