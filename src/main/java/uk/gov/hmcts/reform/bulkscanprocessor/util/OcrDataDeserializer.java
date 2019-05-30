package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;

import java.io.IOException;
import java.util.Base64;

public class OcrDataDeserializer extends StdDeserializer<InputOcrData> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OcrDataDeserializer() {
        super(InputOcrData.class);
    }

    @Override
    public InputOcrData deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext
    ) {
        try {
            return parseOcrData(jsonParser.getText());
        } catch (Exception ex) {
            throw new OcrDataParseException("Failed to parse OCR data", ex);
        }
    }

    private InputOcrData parseOcrData(String base64EncodedOcrData) throws IOException {
        String ocrDataJson = new String(Base64.getDecoder().decode(base64EncodedOcrData));
        return objectMapper.readValue(ocrDataJson, InputOcrData.class);
    }
}
