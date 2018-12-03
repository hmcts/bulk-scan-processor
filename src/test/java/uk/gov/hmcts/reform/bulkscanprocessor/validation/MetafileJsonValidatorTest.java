package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

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
        assertThat(envelope.scannableItems).hasSize(2);
        assertThat(envelope.payments).hasSize(1);
        assertThat(envelope.payments.get(0).amount).isEqualTo(new BigDecimal("100.00"));
        assertThat(envelope.caseNumber).isEqualTo("1111222233334446");
        assertThat(envelope.classification).isEqualTo(Classification.NEW_APPLICATION);
        assertThat(envelope.scannableItems)
            .extracting("documentType")
            .containsExactlyInAnyOrder("Cherished", "Other");
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
    public void should_parse_envelope_data_with_no_account_details_for_postal_payment() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/postal-payment-method.json");

        assertThat(envelope.payments).hasSize(1);
        assertThat(envelope.payments.get(0).amount).isEqualTo(new BigDecimal("100.00"));
        assertThat(envelope.payments.get(0).paymentInstrumentNumber).isEqualTo("1000000000");
        assertThat(envelope.payments.get(0).accountNumber).isNull();
        assertThat(envelope.payments.get(0).sortCode).isNull();
    }

    @Test
    public void should_parse_envelope_data_with_no_payment_reference_number_for_cash_payment() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/cash-payment-method.json");

        assertThat(envelope.payments).hasSize(1);
        assertThat(envelope.payments.get(0).amount).isEqualTo(new BigDecimal("100.00"));
        assertThat(envelope.payments.get(0).paymentInstrumentNumber).isNull();
        assertThat(envelope.payments.get(0).accountNumber).isNull();
        assertThat(envelope.payments.get(0).sortCode).isNull();
    }

    @Test
    public void should_parse_envelope_data_with_account_details_for_cheque_payment() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/cheque-payment-method.json");

        assertThat(envelope.payments).hasSize(1);
        assertThat(envelope.payments.get(0).amount).isEqualTo(new BigDecimal("100.00"));
        assertThat(envelope.payments.get(0).paymentInstrumentNumber).isEqualTo("1000000000");
        assertThat(envelope.payments.get(0).accountNumber).isEqualTo("12345678");
        assertThat(envelope.payments.get(0).sortCode).isEqualTo("112233");
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
    public void should_parse_envelope_data_with_multiple_payment_methods() throws IOException {
        InputEnvelope envelope = getEnvelope("/metafiles/valid/multiple-payment-methods.json");

        assertThat(envelope.payments).hasSize(2);
        assertThat(envelope.payments.get(0).method).isEqualTo("Cheque");
        assertThat(envelope.payments.get(0).amount).isEqualTo(new BigDecimal("200.00"));
        assertThat(envelope.payments.get(0).paymentInstrumentNumber).isEqualTo("1000000000");
        assertThat(envelope.payments.get(0).sortCode).isEqualTo("112233");
        assertThat(envelope.payments.get(0).accountNumber).isEqualTo("12345678");

        assertThat(envelope.payments.get(1).method).isEqualTo("Postal");
        assertThat(envelope.payments.get(1).amount).isEqualTo(new BigDecimal("100.00"));
        assertThat(envelope.payments.get(1).paymentInstrumentNumber).isEqualTo("1000000001");
    }

    private InputEnvelope getEnvelope(String resource) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resource)) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }
}
