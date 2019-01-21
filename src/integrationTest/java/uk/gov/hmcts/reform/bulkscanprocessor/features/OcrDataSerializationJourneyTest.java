package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class OcrDataSerializationJourneyTest {

    @Autowired
    private EnvelopeRepository repository;

    @Test
    public void should_deserialize_all_ocr_fields_in_insertion_order() throws Exception {
        ObjectMapper mapper = new ObjectMapper().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        InputEnvelope inputEnvelope;
        InputStream resourceAsStream = EnvelopeCreator.class.getResourceAsStream(
            "/metafiles/valid/envelope-with-ocr-data.json"
        );

        OcrData expectedResult = new OcrData();
        List<OcrDataField> ocrDataFields = Arrays.asList(
            createOcrDataField(new TextNode("text_field"), new TextNode("some text")),
            createOcrDataField(new TextNode("number_field"), new IntNode(123)),
            createOcrDataField(new TextNode("boolean_field"), BooleanNode.TRUE),
            createOcrDataField(new TextNode("null_field"), NullNode.instance)
        );
        expectedResult.setFields(ocrDataFields);

        try (InputStream inputStream = resourceAsStream) {
            inputEnvelope = mapper.readValue(IOUtils.toByteArray(inputStream), InputEnvelope.class);
        }

        AssertionsForInterfaceTypes.assertThat(inputEnvelope.scannableItems).isNotEmpty();
        AssertionsForInterfaceTypes.assertThat(inputEnvelope.scannableItems.get(0).ocrData)
            .isInstanceOf(OcrData.class);

        Envelope dbEnvelope = EnvelopeMapper.toDbEnvelope(inputEnvelope, "test");
        UUID envelopeId = repository.save(dbEnvelope).getId();

        Envelope readEnvelope = repository.getOne(envelopeId);
        AssertionsForInterfaceTypes.assertThat(readEnvelope.getScannableItems().get(0).getOcrData())
            .isInstanceOf(OcrData.class);

        EnvelopeMsg envelopeMsg = new EnvelopeMsg(readEnvelope);
        AssertionsForInterfaceTypes.assertThat(envelopeMsg.getOcrData()).isInstanceOf(OcrData.class);

        byte[] bytes = mapper.writeValueAsBytes(envelopeMsg);
        JsonNode jsonNode = mapper.readTree(bytes);

        JsonNode ocrData = jsonNode.get("ocr_data");
        OcrData actualValue = mapper.convertValue(
            ocrData,
            OcrData.class
        );
        AssertionsForInterfaceTypes.assertThat(actualValue)
            .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    private OcrDataField createOcrDataField(TextNode fieldName, ValueNode fieldValue) {
        OcrDataField ocrDataField = new OcrDataField();
        ocrDataField.setMetadataFieldName(fieldName);
        ocrDataField.setMetadataFieldValue(fieldValue);
        return ocrDataField;
    }
}
