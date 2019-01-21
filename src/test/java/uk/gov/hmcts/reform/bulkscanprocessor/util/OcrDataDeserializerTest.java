package uk.gov.hmcts.reform.bulkscanprocessor.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class OcrDataDeserializerTest {

    private final OcrDataDeserializer deserializer = new OcrDataDeserializer();

    @Test
    public void should_convert_all_fields_to_their_string_representation() throws Exception {
        OcrData resultOcrData = deserializeFromBase64("/ocr-data/valid/valid-ocr.json");

        OcrData expectedOcrData = new OcrData();
        List<OcrDataField> ocrDataFields = Arrays.asList(
            createOcrDataField(new TextNode("text_field"), new TextNode("some text")),
            createOcrDataField(new TextNode("number_field"), new IntNode(123)),
            createOcrDataField(new TextNode("boolean_field"), BooleanNode.TRUE),
            createOcrDataField(new TextNode("null_field"), NullNode.instance)
        );

        expectedOcrData.setFields(ocrDataFields);

        Assertions.assertThat(resultOcrData).isEqualToComparingFieldByFieldRecursively(expectedOcrData);
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
        expectOcrParsingToFail("/ocr-data/invalid/missing-quotes-of-field-name.txt");
        expectOcrParsingToFail("/ocr-data/invalid/missing-metadata-file-field.json");
        expectOcrParsingToFail("/ocr-data/invalid/null-metadata-file-field.json");
        expectOcrParsingToFail("/ocr-data/invalid/missing-field-name.json");
        expectOcrParsingToFail("/ocr-data/invalid/null-field-name.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-name-not-being-text.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-value-as-array.json");
        expectOcrParsingToFail("/ocr-data/invalid/field-value-as-object.json");
        expectOcrParsingToFail("/ocr-data/invalid/invalid-metadata-file-field.json");
    }

    private void expectOcrParsingToFail(String resourceName) {
        Throwable thrown = catchThrowable(() -> deserializeFromBase64(resourceName));

        assertThat(thrown).isNotNull();
        assertThat(thrown).isInstanceOf(OcrDataParseException.class);
        assertThat(thrown.getMessage()).isEqualTo("Failed to parse OCR data");
    }

    private OcrData deserializeFromBase64(String resourceName) throws IOException {
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

    private OcrDataField createOcrDataField(TextNode fieldName, ValueNode fieldValue) {
        OcrDataField ocrDataField = new OcrDataField();
        ocrDataField.setMetadataFieldName(fieldName);
        ocrDataField.setMetadataFieldValue(fieldValue);
        return ocrDataField;
    }
}
