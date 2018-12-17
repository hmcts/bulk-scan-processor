package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class OcrDataDeserializerTest {

    private final OcrDataDeserializer deserializer = new OcrDataDeserializer();

    @Test
    public void should_convert_all_fields_to_their_string_representation() throws Exception {
        Map<String, String> result = deserializeFromBase64("/ocr-data/valid/valid-ocr.json");

        Map<String, String> expectedResult = ImmutableMap.of(
            "text_field", "some text",
            "number_field", "123",
            "boolean_field", "true",
            "null_field", ""
        );

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void should_throw_illega_state_exception_when_ocr_data_contains_duplicate_keys() {
        Throwable thrown = catchThrowable(() -> {
                JsonParser jsonParser = getJsonParser("/ocr-data/invalid/invalid-ocr.json");
                deserializer.deserialize(jsonParser, mock(DeserializationContext.class));
            }
        );

        assertThat(thrown).isInstanceOf(OcrDataParseException.class)
            .hasCauseExactlyInstanceOf(IllegalStateException.class);

        assertThat(thrown.getCause().getMessage()).contains("Duplicate key for value 'some text'");
    }

    @Test
    public void should_throw_exception_when_ocr_data_is_not_base64() throws Exception {
        Throwable thrown = catchThrowable(() -> {
            JsonParser jsonParser = mock(JsonParser.class);
            given(jsonParser.getText()).willReturn("this is not a Base64 encoded string");
            deserializer.deserialize(jsonParser, mock(DeserializationContext.class));
        });

        assertThat(thrown).isInstanceOf(OcrDataParseException.class);
        assertThat(thrown.getMessage()).isEqualTo("Failed to parse OCR data");
    }

    @Test
    public void should_throw_exception_when_base64_decoded_ocr_data_is_invalid() throws Exception {
        expectOcrParsingToFail("/ocr-data/invalid/not-a-valid-json.txt");
        expectOcrParsingToFail("/ocr-data/invalid/missing-metadata-file-field.json");
        expectOcrParsingToFail("/ocr-data/invalid/null-metadata-file-field.json");
        expectOcrParsingToFail("/ocr-data/invalid/missing-field-name.json");
        expectOcrParsingToFail("/ocr-data/invalid/null-field-name.json");
        expectOcrParsingToFail("/ocr-data/invalid/duplicate-field-name.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-name-not-being-text.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-value-as-array.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-value-as-object.json");
        expectOcrParsingToFail("/ocr-data/invalid/invalid-metadata-file-field.json");
    }

    private void expectOcrParsingToFail(String resourceName) throws IOException {
        Throwable thrown = catchThrowable(() -> deserializeFromBase64(resourceName));

        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(OcrDataParseException.class);
        assertThat(thrown.getMessage()).isEqualTo("Failed to parse OCR data");
    }

    private Map<String, String> deserializeFromBase64(String resourceName) throws IOException {
        JsonParser jsonParser = getJsonParser(resourceName);
        return deserializer.deserialize(jsonParser, mock(DeserializationContext.class));
    }

    private JsonParser getJsonParser(String resourceName) throws IOException {
        JsonParser jsonParser = mock(JsonParser.class);
        given(jsonParser.getText()).willReturn(asBase64(resourceName));
        return jsonParser;
    }

    private String asBase64(String resourceName) throws IOException {
        byte[] fileAsBytes = IOUtils.toByteArray(getClass().getResource(resourceName));
        return java.util.Base64.getEncoder().encodeToString(fileAsBytes);
    }
}
