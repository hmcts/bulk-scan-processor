package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeSchemaException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MetafileJsonValidatorTestForInvalidFiles {

    private static final String SAMPLE_ZIP_FILE_NAME = "zip-file-123";

    private MetafileJsonValidator validator;

    @BeforeEach
    void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @Test
    void should_not_parse_envelope_with_no_scannable_items_in() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/no-scannables.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                + "\n\terror: object has missing required properties"
            )
            .hasMessageContaining("missing: [\"scannable_items\"]");
    }

    @Test
    void should_not_parse_envelope_with_unknown_properties() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/unrecognised-fields.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                + "\n\terror: object instance has properties which are not allowed by the schema: "
                + "[\"invalid_field_name\"]"
            )
            .hasMessageContaining("unwanted: [\"invalid_field_name\"]");

    }

    @Test
    void should_not_parse_envelope_with_missing_top_level_fields() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/missing-top-level-fields.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: object has missing required properties"
            )
            .hasMessageContaining("envelope_classification")
            .hasMessageContaining("jurisdiction")
            .hasMessageContaining("opening_date");
    }

    // log as per pair review request:
    // Failed validation against schema:
    //    object has missing required properties (["envelope_classification","jurisdiction"])
    //    object has missing required properties (["method"])
    //    object has missing required properties (["scanning_date"])
    //    object has missing required properties (["document_type","file_name"])
    @Test
    void should_not_parse_envelope_with_missing_required_fields() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/missing-required-fields.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: object has missing required properties"
            )
            .hasMessageContaining("envelope_classification")
            .hasMessageContaining("jurisdiction")
            .hasMessageContaining("scanning_date")
            .hasMessageContaining("document_type")
            .hasMessageContaining("next_action")
            .hasMessageContaining("next_action_date")
            .hasMessageContaining("document_control_number")
            .hasMessageContaining("file_name");
    }

    @Test
    void should_not_parse_envelope_with_invalid_date_format() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-date-format.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: ECMA 262 regex ")
            .hasMessageContaining("2013-02-20 00:00:00.100000")
            .hasMessageContaining("instance: {\"pointer\":\"/delivery_date\"}")
            .hasMessageContaining("17-13-24T00:00:00.001Z")
            .hasMessageContaining("instance: {\"pointer\":\"/opening_date\"}")
            .hasMessageContaining("2017-02-24T23:00:00.010")
            .hasMessageContaining("instance: {\"pointer\":\"/zip_file_createddate\"}");

    }

    @Test
    void should_not_parse_envelope_with_invalid_zip_file_name_format() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-zip-file-name-format.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: ECMA 262 regex ")
            .hasMessageContaining("1a_24-02-2017-00-00-00.zip")
            .hasMessageContaining("instance: {\"pointer\":\"/zip_file_name\"}");
    }

    @Test
    void should_not_parse_envelope_with_wrong_enum_for_classification_provided() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/enum-boundaries-for-clasification.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                    + "\n\terror: instance value (\"NEW_application\") not found in enum"
            )
            .hasMessageContaining("exception")
            .hasMessageContaining("new_application")
            .hasMessageContaining("supplementary_evidence")
            .hasMessageContaining("instance: {\"pointer\":\"/envelope_classification\"}");
    }

    @Test
    void should_not_parse_envelope_with_wrong_enum_for_document_type_provided() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/enum-boundaries-for-document-type.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                    + "\n\terror: instance value (\"CherIShed\") not found in enum"
            )
            .hasMessageContaining("Cherished")
            .hasMessageContaining("Other")
            .hasMessageContaining("instance: {\"pointer\":\"/scannable_items/0/document_type\"}");
    }

    @Test
    void should_not_parse_envelope_with_specified_non_pdf_file() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/non-pdf-extension.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: ECMA 262 regex ")
            .hasMessageContaining("1111001.gif")
            .hasMessageContaining("instance: {\"pointer\":\"/scannable_items/0/file_name\"}");
    }

    @Test
    void should_not_parse_envelope_with_no_required_fields_in_scannable_items() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-scannable-items.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                    + "\n\terror: object has missing required properties"
            )
            .hasMessageContaining("document_control_number")
            .hasMessageContaining("instance: {\"pointer\":\"/scannable_items/1\"}");
    }

    @Test
    void should_not_parse_envelope_with_no_required_fields_in_non_scannable_items() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-non-scannable-items.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                    + "\n\terror: object has missing required properties"
            )
            .hasMessageContaining("document_control_number")
            .hasMessageContaining("item_type")
            .hasMessageContaining("instance: {\"pointer\":\"/non_scannable_items/0\"}");
    }

    @Test
    void should_not_parse_envelope_with_duplicate_payment_dcn_numbers() throws Exception {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/duplicate-payment-dcns.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(
                getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                    + "\n\terror: array must not contain duplicate element"
            )
            .hasMessageContaining("instance: {\"pointer\":\"/payments\"}");
    }

    @Test
    void should_not_parse_envelope_with_invalid_zip_file_name_in_rescan_for_property() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/invalid-rescan-for-zip-file-name-format.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));

        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME) + "\n\terror: ECMA 262 regex ")
            .hasMessageContaining("1a_24-02-2017-00-00-00.zip")
            .hasMessageContaining("instance: {\"pointer\":\"/rescan_for\"}");
    }

    @Test
    void should_not_parse_envelope_with_long_case_num() throws IOException {
        // given
        byte[] metafile = getMetafile("/metafiles/invalid/long-case-number.json");

        // when
        Throwable exc = catchThrowable(() -> validator.validate(metafile, SAMPLE_ZIP_FILE_NAME));


        // then
        assertThat(exc)
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith(getExpectedErrorHeaderLine(SAMPLE_ZIP_FILE_NAME)
                                        + "\n\terror: string"
            )
            .hasMessageContaining("is too long (length: 116, maximum allowed: 100)");

    }

    private byte[] getMetafile(String resource) throws IOException {
        return IOUtils.toByteArray(getClass().getResource(resource));
    }

    private String getExpectedErrorHeaderLine(String zipFilename) {
        return String.format("Failed validation for file %s against schema. Errors:", zipFilename);
    }
}
