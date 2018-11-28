package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class OcrDataParserTest {

    private final OcrDataParser ocrDataParser = new OcrDataParser();

    @Test
    public void should_convert_all_fields_to_their_string_representation() throws Exception {
        String base64data = asBase64("/ocr-data/valid-ocr.json");

        Map<String, String> result = ocrDataParser.parseOcrData(base64data);

        Map<String, String> expectedResult = ImmutableMap.of(
            "text_field", "some text",
            "number_field", "123",
            "boolean_field", "true",
            "null_field", ""
        );

        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void should_throw_exception_when_ocr_data_is_not_base64() {
        Throwable thrown = catchThrowable(() ->
            // parsing data that's not base64-encoded
            ocrDataParser.parseOcrData("/ocr-data/valid-ocr.json")
        );

        assertThat(thrown).isInstanceOf(OcrDataParseException.class);
        assertThat(thrown.getMessage()).isEqualTo("Failed to parse OCR data");
    }

    @Test
    public void should_throw_exception_when_base64_decoded_ocr_data_is_valid() throws Exception {
        List<String> incorrectOcrResourceNames = Arrays.asList(
            "/ocr-data/not-a-valid-json.txt",
            "/ocr-data/missing-metadata-file-field.json",
            "/ocr-data/null-metadata-file-field.json",
            "/ocr-data/null-metadata-file-field.json",
            "/ocr-data/invalid-metadata-file-field.json",
            "/ocr-data/duplicate-field-name.json"
        );

        for (String resourceName : incorrectOcrResourceNames) {
            expectOcrParsingToFail(resourceName);
        }
    }

    private void expectOcrParsingToFail(String resourceName) throws IOException {
        String base64data = asBase64(resourceName);

        Throwable thrown = catchThrowable(() -> ocrDataParser.parseOcrData(base64data));

        assertThat(thrown).isInstanceOf(OcrDataParseException.class);
        assertThat(thrown.getMessage()).isEqualTo("Failed to parse OCR data");
    }

    private String asBase64(String resourceName) throws IOException {
        byte[] fileAsBytes = IOUtils.toByteArray(getClass().getResource(resourceName));
        return java.util.Base64.getEncoder().encodeToString(fileAsBytes);
    }
}
