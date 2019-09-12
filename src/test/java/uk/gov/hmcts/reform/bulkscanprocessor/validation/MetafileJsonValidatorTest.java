package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class MetafileJsonValidatorTest {

    private MetafileJsonValidator validator;

    @Before
    public void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @Test
    public void should_successfully_map_json_file_to_entities() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/from-spec.json");

        assertThat(envelope.nonScannableItems).hasSize(1);
        assertThat(envelope.scannableItems).hasSize(4);
        assertThat(envelope.payments).hasSize(1);
        assertThat(envelope.payments.get(0).documentControlNumber).isEqualTo("1111002");
        assertThat(envelope.caseNumber).isEqualTo("1111222233334446");
        assertThat(envelope.classification).isEqualTo(Classification.NEW_APPLICATION);
        assertThat(envelope.scannableItems)
            .extracting(item -> item.documentType)
            .containsExactlyInAnyOrder(
                InputDocumentType.CHERISHED,
                InputDocumentType.OTHER,
                InputDocumentType.SSCS1,
                InputDocumentType.COVERSHEET
            );
    }

    @Test
    public void should_parse_envelope_data_with_no_case_number() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/no-case-number.json");

        assertThat(envelope.caseNumber).isNull();
        assertThat(envelope.nonScannableItems).hasSize(1);
        assertThat(envelope.scannableItems).hasSize(2);
        assertThat(envelope.payments).hasSize(1);
    }

    @Test
    public void should_parse_envelope_data_with_no_previous_service_case_reference() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/no-previous-service-case-reference.json");
        assertThat(envelope.previousServiceCaseReference).isNull();
    }

    @Test
    public void should_parse_envelope_data_with_null_values_for_non_mandatory_fields() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/fields-with-null-values-non-mandatory.json");

        assertThat(envelope.caseNumber).isNull();
        assertThat(envelope.previousServiceCaseReference).isNull();
        assertThat(envelope.nonScannableItems).hasSize(1);
        assertThat(envelope.scannableItems).hasSize(2);
        assertThat(envelope.payments).hasSize(1);
    }

    @Test
    public void should_parse_envelope_data_with_no_payments_in() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/no-payment.json");

        assertThat(envelope.nonScannableItems).hasSize(1);
        assertThat(envelope.scannableItems).hasSize(2);
        assertThat(envelope.payments).hasSize(0);
    }

    @Test
    public void should_parse_envelope_data_with_no_non_scannable_items_in() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/no-non-scannables.json");

        assertThat(envelope.nonScannableItems).hasSize(0);
        assertThat(envelope.scannableItems).hasSize(2);
        assertThat(envelope.payments).hasSize(1);
    }

    @Test
    public void should_parse_envelope_with_non_scannable_items() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/from-spec.json");

        assertThat(envelope.nonScannableItems).hasSize(1);
        assertThat(envelope.nonScannableItems.get(0).documentControlNumber).isEqualTo("1111001");
        assertThat(envelope.nonScannableItems.get(0).itemType).isEqualTo("CD");
        assertThat(envelope.nonScannableItems.get(0).notes).isEqualTo("4GB USB memory stick");
    }

    @Test
    public void should_parse_envelope_data_with_payments() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/with-payments.json");

        assertThat(envelope.payments).hasSize(2);
        assertThat(envelope.payments.get(0).documentControlNumber).isEqualTo("1111001");
        assertThat(envelope.payments.get(1).documentControlNumber).isEqualTo("1111002");
    }

    private InputEnvelope getEnvelope(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }
}
