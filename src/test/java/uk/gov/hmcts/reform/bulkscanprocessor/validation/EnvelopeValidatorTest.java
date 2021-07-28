package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("checkstyle:LineLength")
class EnvelopeValidatorTest {

    private static final String VALIDATION_URL = "https://example.com/validate-ocr";
    private static final String PO_BOX_1 = "sample PO box 1";
    private static final String PO_BOX_2 = "sample PO box 2";
    private static final String JURISDICTION = "jurisdiction";
    private static final String CONTAINER = "container";

    private EnvelopeValidator envelopeValidator;

    @BeforeEach
    void setUp() {
        envelopeValidator = new EnvelopeValidator();
    }

    @Test
    void assertEnvelopeContainsDocsOfAllowedTypesOnly_should_pass() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                Classification.EXCEPTION,
                singletonList(InputEnvelopeCreator.scannableItem("file1", InputDocumentType.OTHER))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope));
    }

    @Test
    void assertEnvelopeContainsDocsOfAllowedTypesOnly_should_pass_for_supplementary_evidence() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                Classification.SUPPLEMENTARY_EVIDENCE,
                singletonList(InputEnvelopeCreator.scannableItem("file1", InputDocumentType.OTHER))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope));
    }

    @ParameterizedTest
    @EnumSource(
            value = InputDocumentType.class,
            names = {"FORM", "SSCS1"}
    )
    void assertEnvelopeContainsDocsOfAllowedTypesOnly_should_throw_for_disallowed_document_type(InputDocumentType documentType) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                Classification.SUPPLEMENTARY_EVIDENCE,
                singletonList(InputEnvelopeCreator.scannableItem("file1", documentType))
        );

        // when
        // then
        assertThrows(
            DisallowedDocumentTypesException.class,
            () -> envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesOnly(envelope)
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"EXCEPTION", "SUPPLEMENTARY_EVIDENCE"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_pass_for_allowed_classification(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem("file1", InputDocumentType.OTHER))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope));
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_pass_for_allowed_with_default_document_type(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.FORM, getOcrData()))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope));
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_throw_if_no_documents_should_have_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.CHERISHED, getOcrData()))
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        )
                .isInstanceOf(OcrDataNotFoundException.class)
                .hasMessage("No documents of type Form found");
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_throw_if_form_document_has_no_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.FORM, null))
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        )
                .isInstanceOf(OcrDataNotFoundException.class)
                .hasMessage("Missing OCR data");
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_throw_if_form_document_has_empty_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "BULKSCAN",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.FORM, new InputOcrData()))
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        )
                .isInstanceOf(OcrDataNotFoundException.class)
                .hasMessage("Missing OCR data");
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_pass_if_sscs_document_has_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.SSCS1, getOcrData()))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope));
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_throw_if_sscs1_document_has_no_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.SSCS1, null))
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        )
                .isInstanceOf(OcrDataNotFoundException.class)
                .hasMessage("Missing OCR data");
    }

    @ParameterizedTest
    @EnumSource(
            value = Classification.class,
            names = {"NEW_APPLICATION", "SUPPLEMENTARY_EVIDENCE_WITH_OCR"}
    )
    void assertEnvelopeContainsOcrDataIfRequired_should_throw_if_sscs1_document_has_empty_ocr_data(Classification classification) {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                classification,
                singletonList(InputEnvelopeCreator.scannableItem(InputDocumentType.SSCS1, new InputOcrData()))
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        )
                .isInstanceOf(OcrDataNotFoundException.class)
                .hasMessage("Missing OCR data");
    }

    @Test
    void assertEnvelopeHasPdfs() {
        // given


        // when


        // then

    }

    @Test
    void assertDocumentControlNumbersAreUnique() {
        // given


        // when


        // then

    }

    @Test
    void assertZipFilenameMatchesWithMetadata() {
        // given


        // when


        // then

    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox() {
        // given


        // when


        // then

    }

    @Test
    void assertPaymentsEnabledForContainerIfPaymentsArePresent() {
        // given


        // when


        // then

    }

    @Test
    void assertServiceEnabled() {
        // given


        // when


        // then

    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_passes_if_jurisdiction_and_pobox_are_correct() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_1.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                        singletonList(m),
                        envelope,
                        CONTAINER
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_passes_if_jurisdiction_and_pobox_are_correct_multiple_poboxes() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                asList(PO_BOX_1, PO_BOX_2),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                        singletonList(m),
                        envelope,
                        CONTAINER
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_throws_if_pobox_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThrows(
            ContainerJurisdictionPoBoxMismatchException.class,
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    singletonList(m),
                    envelope,
                    CONTAINER
            )
        );
    }

    @Test
    void assertContainerMatchesJurisdictionAndPoBox_throws_if_jurisdiction_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope("wrong", PO_BOX_1.toUpperCase());

        // when
        // then
        assertThrows(
            ContainerJurisdictionPoBoxMismatchException.class,
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    singletonList(m),
                    envelope,
                    CONTAINER
            )
        );
    }

    @Test
    void assertServiceEnabled_passes_if_pobox_is_correct() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_1.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        singletonList(m)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertServiceEnabled_passes_if_pobox_is_correct_multiple_poboxes() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                asList(PO_BOX_1, PO_BOX_2),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThatCode(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        singletonList(m)
                )
        ).doesNotThrowAnyException();
    }

    @Test
    void assertServiceEnabled_throws_if_pobox_is_incorrect() {
        // given
        ContainerMappings.Mapping m = new ContainerMappings.Mapping(
                CONTAINER,
                JURISDICTION,
                singletonList(PO_BOX_1),
                VALIDATION_URL,
                true,
                true
        );
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(JURISDICTION, PO_BOX_2.toUpperCase());

        // when
        // then
        assertThrows(
            ServiceDisabledException.class,
            () -> envelopeValidator.assertServiceEnabled(
                    envelope,
                    singletonList(m)
            )
        );
    }

    private InputOcrData getOcrData() {
        InputOcrData ocrData = new InputOcrData();
        ocrData.setFields(singletonList(new InputOcrDataField(new TextNode("foo"), new TextNode("bar"))));
        return ocrData;
    }
}
