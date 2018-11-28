package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;

import java.io.IOException;
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
                    field -> field.getName(),
                    field -> field.getValue().asText("")
                )
            );
        } catch (IllegalArgumentException | IOException ex) {
            throw new OcrDataParseException("Invalid OCR data", ex);
        }
    }
}
