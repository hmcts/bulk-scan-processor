package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;

import java.io.IOException;
import java.util.Base64;

public class OcrDataDeserializer extends StdDeserializer<OcrData> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OcrDataDeserializer() {
        super(OcrData.class);
    }

    @Override
    public OcrData deserialize(
        JsonParser jsonParser,
        DeserializationContext deserializationContext
    ) {
        try {
            return parseOcrData(jsonParser.getText());
        } catch (Exception ex) {
            throw new OcrDataParseException("Failed to parse OCR data", ex);
        }
    }

    private OcrData parseOcrData(String base64EncodedOcrData) throws IOException {
        String ocrDataJson = new String(Base64.getDecoder().decode(base64EncodedOcrData));

        OcrData ocrData = objectMapper.readValue(ocrDataJson, OcrData.class);
        ocrData.getFields().forEach(
            ocrDataField -> {
                if (ocrDataField.getName().isNull()) {
                    throw new IllegalStateException("Ocr data field name must be provided.");
                }
            }
        );
        return ocrData;
    }
}
