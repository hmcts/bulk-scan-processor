package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class OcrDataDeserializer extends StdDeserializer<Map<String, String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OcrDataDeserializer() {
        super(LinkedHashMap.class);
    }

    @Override
    public Map<String, String> deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext
    ) throws IOException {
        try {
            return parseOcrData(jsonParser.getText());
        } catch (Exception ex) {
            throw new OcrDataParseException("Failed to parse OCR data", ex);
        }
    }

    private Map<String, String> parseOcrData(String base64EncodedOcrData) throws IOException {
        String ocrDataJson = new String(Base64.getDecoder().decode(base64EncodedOcrData));

        OcrData ocrData = objectMapper.readValue(ocrDataJson, OcrData.class);

        return ocrData.getFields().stream().collect(
            toMap(
                field -> field.getName().textValue(),
                field -> field.getValue().asText(""),
                (value1, value2) -> {
                    throw new IllegalStateException(String.format("Ocr data contains duplicate fields"));
                },
                LinkedHashMap::new
            )
        );
    }
}
