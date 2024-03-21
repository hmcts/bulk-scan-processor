package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;

import java.io.IOException;
import java.util.Base64;

/**
 * Deserializer for OCR data.
 */
public class OcrDataDeserializer extends StdDeserializer<InputOcrData> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates an instance of InputOcrData deserializer.
     */
    public OcrDataDeserializer() {
        super(InputOcrData.class);
    }

    /**
     * Deserializes OCR data from a base64 encoded string.
     *
     * @param jsonParser the parser
     * @param deserializationContext the context
     * @return the OCR data
     * @throws OcrDataParseException if there is an error parsing the OCR data
     */
    @Override
    public InputOcrData deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext
    ) throws OcrDataParseException {
        try {
            return parseOcrData(jsonParser.getText());
        } catch (Exception ex) {
            throw new OcrDataParseException(jsonParser, "Failed to parse OCR data", ex);
        }
    }

    /**
     * Parses OCR data from a base64 encoded string.
     *
     * @param base64EncodedOcrData the base64 encoded OCR data
     * @return the OCR data
     * @throws IOException if there is an error parsing the OCR data
     */
    private InputOcrData parseOcrData(String base64EncodedOcrData) throws IOException {
        String ocrDataJson = new String(Base64.getDecoder().decode(base64EncodedOcrData));
        return objectMapper.readValue(ocrDataJson, InputOcrData.class);
    }
}
