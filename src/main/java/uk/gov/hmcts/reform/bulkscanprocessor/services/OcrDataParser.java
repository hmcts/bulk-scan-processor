package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;

import java.util.Base64;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class OcrDataParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, String> parseOcrData(String base64EncodedOcrData) {
        try {
            String ocrDataJson = new String(Base64.getDecoder().decode(base64EncodedOcrData));

            OcrData ocrData = objectMapper.readValue(ocrDataJson, OcrData.class);

            return ocrData.getFields().stream().collect(
                toMap(
                    field -> field.getName().textValue(),
                    field -> field.getValue().asText("")
                )
            );
        } catch (Exception ex) {
            throw new OcrDataParseException("Failed to parse OCR data", ex);
        }
    }
}
