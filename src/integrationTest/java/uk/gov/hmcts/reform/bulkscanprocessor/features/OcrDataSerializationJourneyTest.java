package uk.gov.hmcts.reform.bulkscanprocessor.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.OcrField;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;

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

        try (InputStream inputStream = resourceAsStream) {
            inputEnvelope = mapper.readValue(IOUtils.toByteArray(inputStream), InputEnvelope.class);
        }

        AssertionsForInterfaceTypes.assertThat(inputEnvelope.scannableItems).isNotEmpty();
        AssertionsForInterfaceTypes.assertThat(inputEnvelope.scannableItems.get(0).ocrData)
            .isInstanceOf(InputOcrData.class);

        Envelope dbEnvelope = EnvelopeMapper.toDbEnvelope(
            inputEnvelope,
            "test",
            Optional.of(
                new OcrValidationWarnings(
                    inputEnvelope.scannableItems.get(0).documentControlNumber,
                    singletonList("warning 1")
                )
            )
        );

        UUID envelopeId = repository.saveAndFlush(dbEnvelope).getId();

        Envelope readEnvelope = repository.getOne(envelopeId);
        AssertionsForInterfaceTypes.assertThat(readEnvelope.getScannableItems().get(0).getOcrData())
            .isInstanceOf(OcrData.class);

        EnvelopeMsg envelopeMsg = new EnvelopeMsg(readEnvelope);

        byte[] bytes = mapper.writeValueAsBytes(envelopeMsg);
        JsonNode jsonNode = mapper.readTree(bytes);

        JsonNode ocrData = jsonNode.get("ocr_data");

        OcrField[] expecteOcrFields = {
            new OcrField("text_field", "some text"),
            new OcrField("number_field", "123"),
            new OcrField("boolean_field", "true"),
            new OcrField("null_field", "")
        };

        OcrField[] actualOcrFields = mapper.convertValue(ocrData, OcrField[].class);

        AssertionsForInterfaceTypes.assertThat(actualOcrFields)
            .usingFieldByFieldElementComparator()
            .isEqualTo(expecteOcrFields);
    }
}
