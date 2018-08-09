package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityParserTest {

    @Test
    public void should_successfully_map_json_file_to_entities() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile.json");
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
    }

    @Test
    public void should_parse_envelop_data_with_no_payments_in() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile-no-payment.json");
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(0);
    }

    @Test
    public void should_parse_envelop_data_with_no_non_scannable_items_in() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile-no-non-scannables.json");
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getNonScannableItems()).hasSize(0);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
    }

    @Test
    public void should_parse_envelop_data_with_no_scannable_items_in() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile-no-scannables.json");
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(0);
        assertThat(envelope.getPayments()).hasSize(1);
    }

    @Test
    public void should_parse_envelop_data_with_updated_spec_fields() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/metafile-model-update-rpe610.json");
        Envelope envelope = EntityParser.parseEnvelopeMetadata(inputStream);

        assertThat(envelope.getUrgent()).isTrue();
        assertThat(envelope.getClassification()).isEqualTo(Classification.NEW_APPLICATION);
        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getScannableItems())
            .extracting("documentType")
            .containsExactlyInAnyOrder("Passport", null);
        assertThat(envelope.getPayments()).hasSize(1);
    }

}
