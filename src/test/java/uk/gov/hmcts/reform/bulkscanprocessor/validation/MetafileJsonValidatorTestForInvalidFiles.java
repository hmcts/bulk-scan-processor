package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeSchemaException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class MetafileJsonValidatorTestForInvalidFiles {

    private MetafileJsonValidator validator;

    @Before
    public void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @Test
    public void should_not_parse_envelope_with_no_scannable_items_in() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/no-scannables.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tobject has missing required properties")
            .hasMessageContaining("scannable_items");
    }

    @Test
    public void should_not_parse_envelope_with_missing_top_level_fields() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/missing-top-level-fields.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tobject has missing required properties")
            .hasMessageContaining("classification")
            .hasMessageContaining("jurisdiction")
            .hasMessageContaining("opening_date");
    }

    // log as per pair review request:
    // Failed validation against schema:
    //    object has missing required properties (["classification","jurisdiction"])
    //    object has missing required properties (["method"])
    //    object has missing required properties (["scanning_date"])
    //    object has missing required properties (["document_type","file_name"])
    @Test
    public void should_not_parse_envelope_with_missing_required_fields() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/missing-required-fields.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tobject has missing required properties")
            .hasMessageContaining("classification")
            .hasMessageContaining("jurisdiction")
            .hasMessageContaining("scanning_date")
            .hasMessageContaining("document_type")
            .hasMessageContaining("next_action")
            .hasMessageContaining("next_action_date")
            .hasMessageContaining("document_control_number")
            .hasMessageContaining("file_name");
    }

    @Test
    public void should_not_parse_envelope_with_invalid_date_format() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-date-format.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tECMA 262 regex ")
            .hasMessageContaining("2013-02-20 00:00:00.100000");
    }

    @Test
    public void should_not_parse_envelope_with_invalid_zip_file_name_format() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-zip-file-name-format.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tECMA 262 regex ")
            .hasMessageContaining("1a_24-02-2017-00-00-00.zip");
    }

    @Test
    public void should_not_parse_envelope_with_wrong_enum_value_provided() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/enum-boundaries-for-clasification.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\t"
                + "instance value (\"NEW_application\") not found in enum")
            .hasMessageContaining("exception")
            .hasMessageContaining("new_application")
            .hasMessageContaining("supplementary_evidence");
    }

    @Test
    public void should_not_parse_envelope_with_specified_non_pdf_file() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/non-pdf-extension.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tECMA 262 regex ")
            .hasMessageContaining("1111001.gif");
    }

    // Failed validation against schema:
    //    instance value (false) not found in enum (possible values: ["false","true"])
    //    instance type (boolean) does not match any allowed primitive type (allowed: ["string"])
    @Test
    public void should_not_parse_envelope_with_direct_usage_of_boolean() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/use-of-boolean.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tinstance value (false) not found in enum")
            .hasMessageContaining("instance type (boolean) does not match any allowed primitive type")
            .hasMessageContaining("\"false\"")
            .hasMessageContaining("\"true\"");
    }

    @Test
    public void should_not_parse_envelope_with_direct_usage_of_numeric() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/use-of-numeric.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\t"
                + "instance failed to match exactly one schema (matched 0 out of 3)");
    }

    @Test
    public void should_not_parse_envelope_with_no_required_fields_for_cheque_payment() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-cheque-payment.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\t"
                + "instance failed to match exactly one schema (matched 0 out of 3)");
    }

    @Test
    public void should_not_parse_envelope_with_no_payment_instrument_number_for_postal_payment() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-postal-order-payment.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\t"
                + "instance failed to match exactly one schema (matched 0 out of 3)");
    }

    @Test
    public void should_not_parse_envelope_with_no_required_fields_in_scannable_items() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-scannable-items.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tobject has missing required properties")
            .hasMessageContaining("document_control_number");
    }

    @Test
    public void should_not_parse_envelope_with_no_required_fields_in_non_scannable_items() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-non-scannable-items.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\tobject has missing required properties")
            .hasMessageContaining("document_control_number")
            .hasMessageContaining("item_type");
    }

    private byte[] getMetafile(String resource) throws IOException {
        return IOUtils.toByteArray(getClass().getResource(resource));
    }
}
