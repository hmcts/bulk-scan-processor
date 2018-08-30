package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.rule.OutputCapture;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeSchemaException;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

public class MetafileJsonValidatorTestForInvalidFiles {

    @Rule
    public OutputCapture capture = new OutputCapture();

    private MetafileJsonValidator validator;

    @Before
    public void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @After
    public void tearDown() {
        capture.flush();
    }

    @Test
    public void should_not_parse_envelope_with_no_scannable_items_in() {
        tryToGetEnvelope(
            "/metafiles/invalid/no-scannables.json",
            "object has missing required properties",
            ImmutableList.of("scannable_items")
        );
    }

    @Test
    public void should_not_parse_envelope_with_missing_top_level_fields() {
        tryToGetEnvelope(
            "/metafiles/invalid/missing-top-level-fields.json",
            "object has missing required properties",
            ImmutableList.of("classification", "jurisdiction", "opening_date")
        );
    }

    @Test
    public void should_not_parse_envelope_with_missing_required_fields() {
        // log as per pair review request:
        // Failed validation against schema:
        //    object has missing required properties (["classification","jurisdiction"])
        //    object has missing required properties (["method"])
        //    object has missing required properties (["scanning_date"])
        //    object has missing required properties (["document_type","file_name"])
        tryToGetEnvelope(
            "/metafiles/invalid/missing-required-fields.json",
            "object has missing required properties",
            ImmutableList.of("classification", "jurisdiction", "method", "scanning_date", "document_type", "file_name")
        );
    }

    @Test
    public void should_not_parse_envelope_with_invalid_date_format() {
        tryToGetEnvelope(
            "/metafiles/invalid/invalid-date-format.json",
            "ECMA 262 regex ",
            ImmutableList.of("2013-02-20 00:00:00.100000")
        );
    }

    @Test
    public void should_not_parse_envelope_with_invalid_zip_file_name_format() {
        tryToGetEnvelope(
            "/metafiles/invalid/invalid-zip-file-name-format.json",
            "ECMA 262 regex ",
            ImmutableList.of("1a_24-02-2017-00-00-00.zip")
        );
    }

    @Test
    public void should_not_parse_envelope_with_wrong_enum_value_provided() {
        tryToGetEnvelope(
            "/metafiles/invalid/enum-boundaries-for-clasification.json",
            "instance value (\"NEW_application\") not found in enum",
            ImmutableList.of("exception", "new_application", "supplementary_evidence")
        );
    }

    @Test
    public void should_not_parse_envelope_with_specified_non_pdf_file() {
        tryToGetEnvelope(
            "/metafiles/invalid/non-pdf-extension.json",
            "ECMA 262 regex ",
            ImmutableList.of("1111001.gif")
        );
    }

    @Test
    public void should_not_parse_envelope_with_direct_usage_of_boolean() {
        // Failed validation against schema:
        //    instance value (false) not found in enum (possible values: ["false","true"])
        //    instance type (boolean) does not match any allowed primitive type (allowed: ["string"])
        tryToGetEnvelope(
            "/metafiles/invalid/use-of-boolean.json",
            "instance value (false) not found in enum",
            ImmutableList.of("\"false\"", "\"true\"")
        );
    }

    @Test
    public void should_not_parse_envelope_with_direct_usage_of_numeric() {
        tryToGetEnvelope(
            "/metafiles/invalid/use-of-numeric.json",
            "instance type (number) does not match any allowed primitive type",
            ImmutableList.of("\"string\"")
        );
    }

    private void tryToGetEnvelope(
        String resource,
        String violationMessage,
        List<String> extraMessages
    ) {
        AbstractThrowableAssert assertion = assertThatCode(() -> {
            validator.validate(IOUtils.toByteArray(getClass().getResource(resource)));
        })
            .isInstanceOf(InvalidEnvelopeSchemaException.class)
            .hasMessageStartingWith("Failed validation against schema:\n\t" + violationMessage);

        extraMessages.forEach(assertion::hasMessageContaining);
    }
}
