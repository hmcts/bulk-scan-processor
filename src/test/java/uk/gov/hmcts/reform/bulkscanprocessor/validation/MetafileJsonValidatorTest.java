package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class MetafileJsonValidatorTest {

    private MetafileJsonValidator validator;

    @Before
    public void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @Test
    public void should_successfully_map_json_file_to_entities() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/from-spec.json");

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
        assertThat(envelope.getCaseNumber()).isEqualTo("1111222233334446");
        assertThat(envelope.getClassification()).isEqualTo(Classification.NEW_APPLICATION);
        assertThat(envelope.getScannableItems())
            .extracting("documentType")
            .containsExactlyInAnyOrder(DocumentType.CHERISHED, DocumentType.OTHER);
    }

    @Test
    public void should_parse_envelope_data_with_no_case_number() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/no-case-number.json");

        assertThat(envelope.getCaseNumber()).isNull();
        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
    }

    @Test
    public void should_parse_envelope_data_with_no_payments_in() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/no-payment.json");

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(0);
    }

    @Test
    public void should_parse_envelope_data_with_no_non_scannable_items_in() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/no-non-scannables.json");

        assertThat(envelope.getNonScannableItems()).hasSize(0);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getPayments()).hasSize(1);
    }

    @Test
    public void should_parse_envelope_data_with_no_account_details_for_postal_payment() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/postal-payment-method.json");

        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
        assertThat(envelope.getPayments().get(0).getPaymentInstrumentNumber()).isEqualTo("1000000000");
        assertThat(envelope.getPayments().get(0).getAccountNumber()).isNull();
        assertThat(envelope.getPayments().get(0).getSortCode()).isNull();
    }

    @Test
    public void should_parse_envelope_data_with_no_payment_reference_number_for_cash_payment() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/cash-payment-method.json");

        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
        assertThat(envelope.getPayments().get(0).getPaymentInstrumentNumber()).isNull();
        assertThat(envelope.getPayments().get(0).getAccountNumber()).isNull();
        assertThat(envelope.getPayments().get(0).getSortCode()).isNull();
    }

    @Test
    public void should_parse_envelope_data_with_account_details_for_cheque_payment() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/cheque-payment-method.json");

        assertThat(envelope.getPayments()).hasSize(1);
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(100.0);
        assertThat(envelope.getPayments().get(0).getPaymentInstrumentNumber()).isEqualTo("1000000000");
        assertThat(envelope.getPayments().get(0).getAccountNumber()).isEqualTo("12345678");
        assertThat(envelope.getPayments().get(0).getSortCode()).isEqualTo("112233");
    }

    @Test
    public void should_parse_envelope_with_non_scannable_items() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/from-spec.json");

        assertThat(envelope.getNonScannableItems()).hasSize(1);
        assertThat(envelope.getNonScannableItems().get(0).getDocumentControlNumber()).isEqualTo("1111001");
        assertThat(envelope.getNonScannableItems().get(0).getItemType()).isEqualTo("CD");
        assertThat(envelope.getNonScannableItems().get(0).getNotes()).isEqualTo("4GB USB memory stick");
    }

    @Test
    public void should_parse_envelope_data_with_multiple_payment_methods() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/multiple-payment-methods.json");

        assertThat(envelope.getPayments()).hasSize(2);
        assertThat(envelope.getPayments().get(0).getMethod()).isEqualTo("Cheque");
        assertThat(envelope.getPayments().get(0).getAmount()).isEqualTo(200.0);
        assertThat(envelope.getPayments().get(0).getPaymentInstrumentNumber()).isEqualTo("1000000000");
        assertThat(envelope.getPayments().get(0).getSortCode()).isEqualTo("112233");
        assertThat(envelope.getPayments().get(0).getAccountNumber()).isEqualTo("12345678");

        assertThat(envelope.getPayments().get(1).getMethod()).isEqualTo("Postal");
        assertThat(envelope.getPayments().get(1).getAmount()).isEqualTo(100.0);
        assertThat(envelope.getPayments().get(1).getPaymentInstrumentNumber()).isEqualTo("1000000001");
    }

    @Test
    public void should_parse_envelope_with_case_insensitive_document_type() throws IOException {
        Envelope envelope = getEnvelope("/metafiles/valid/case-insensitive-document-type.json");

        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getScannableItems())
            .extracting("documentType")
            .containsOnly(DocumentType.CHERISHED, DocumentType.OTHER);
    }

    private Envelope getEnvelope(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }
}
