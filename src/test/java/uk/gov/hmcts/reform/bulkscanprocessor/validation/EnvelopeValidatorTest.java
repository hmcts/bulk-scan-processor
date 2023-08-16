package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumbersInEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipNameNotMatchingMetaDataException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.ZIP_FILE_NAME;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;

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
                singletonList(scannableItem("file1", InputDocumentType.OTHER))
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
                SUPPLEMENTARY_EVIDENCE,
                singletonList(scannableItem("file1", InputDocumentType.OTHER))
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
                SUPPLEMENTARY_EVIDENCE,
                singletonList(scannableItem("file1", documentType))
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
                singletonList(scannableItem("file1", InputDocumentType.OTHER))
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
                singletonList(scannableItem(InputDocumentType.FORM, getOcrData()))
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
                singletonList(scannableItem(InputDocumentType.CHERISHED, getOcrData()))
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
                singletonList(scannableItem(InputDocumentType.FORM, null))
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
                singletonList(scannableItem(InputDocumentType.FORM, new InputOcrData()))
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
                singletonList(scannableItem(InputDocumentType.SSCS1, getOcrData()))
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
                singletonList(scannableItem(InputDocumentType.SSCS1, null))
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
                singletonList(scannableItem(InputDocumentType.SSCS1, new InputOcrData()))
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
    void assertEnvelopeHasPdfs_should_pass_for_valid_pdfs() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                asList(scannableItem("file1.pdf", "dcn1"), scannableItem("file2.pdf", "dcn2"))
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeHasPdfs(envelope, asList("file1.pdf", "file2.pdf")));
    }

    @Test
    void assertEnvelopeHasPdfs_should_throw_for_not_declared_pdfs() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                singletonList(scannableItem("file2.pdf", "dcn2"))
        );

        final List<String> pdfs = asList("file1.pdf", "file2.pdf");

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs)
        )
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessage("Not declared PDFs: file1.pdf");
    }

    @Test
    void assertEnvelopeHasPdfs_should_throw_for_missing_pdfs() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                asList(
                        scannableItem("file1.pdf", "dcn1"),
                        scannableItem("file2.pdf", "dcn2")
                )
        );

        // when
        final List<String> pdfs = singletonList("file2.pdf");

        // then
        assertThatThrownBy(
            () -> {
                envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs);
            }
        )
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessage("Missing PDFs: file1.pdf");
    }

    @Test
    void assertDocumentControlNumbersAreUnique_should_pass_for_correct_dcns() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                asList(
                        scannableItem("file1.pdf", "dcn1"),
                        scannableItem("file2.pdf", "dcn2")
                )
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertDocumentControlNumbersAreUnique(envelope));
    }

    @Test
    void assertDocumentControlNumbersAreUnique_should_throw_for_duplicate_dcns() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                asList(
                        scannableItem("file1.pdf", "dcn1"),
                        scannableItem("file2.pdf", "dcn1"),
                        scannableItem("file3.pdf", "dcn2"),
                        scannableItem("file4.pdf", "dcn2"),
                        scannableItem("file5.pdf", "dcn3")
                )
        );

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertDocumentControlNumbersAreUnique(envelope)
        )
            .isInstanceOf(DuplicateDocumentControlNumbersInEnvelopeException.class)
            .hasMessage("Duplicate DCNs in envelope: dcn1, dcn2");
    }

    @Test
    void assertZipFilenameMatchesWithMetadata_should_pass_for_correct_zip_filename() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope("SSCS");

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, ZIP_FILE_NAME));
    }

    @Test
    void assertZipFilenameMatchesWithMetadata_should_throw_for_incorrect_zip_filename() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope("SSCS");

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, "wrong.zip")
        )
            .isInstanceOf(ZipNameNotMatchingMetaDataException.class)
            .hasMessage("Name of the uploaded zip file does not match with field \"zip_file_name\" in the metadata");
    }

    @Test
    void assertPaymentsEnabledForContainerIfPaymentsArePresent_should_pass_if_no_payments() {
        // given
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                emptyList(),
                emptyList()
        );

        final List<ContainerMappings.Mapping> mappings =
                singletonList(
                        new ContainerMappings.Mapping("sscs", "SSCS", singletonList("POBOX"), "http://url", false, true)
                );

        // when
        // then
        assertDoesNotThrow(() ->
                envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                        envelope,
                        false,
                        mappings
                )
        );
    }

    @Test
    void assertPaymentsEnabledForContainerIfPaymentsArePresent_should_pass_if_there_are_payments_and_payments_enabled() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                emptyList(),
                singletonList(new InputPayment("dcn1"))
        );

        final List<ContainerMappings.Mapping> mappings =
                singletonList(
                        new ContainerMappings.Mapping("sscs", "SSCS", singletonList("POBOX"), "http://url", true, true)
                );

        // when
        // then
        assertDoesNotThrow(() ->
                envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                        envelope,
                        true,
                        mappings
                )
        );
    }

    @Test
    void assertPaymentsEnabledForContainerIfPaymentsArePresent_should_throw_if_there_are_payments_and_payments_disabled() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                emptyList(),
                singletonList(new InputPayment("dcn1"))
        );

        final List<ContainerMappings.Mapping> mappings =
                singletonList(
                        new ContainerMappings.Mapping("sscs", "SSCS", singletonList("POBOX"), "http://url", true, true)
                );

        // when
        // then
        assertThatThrownBy(
            () ->
                envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                    envelope,
                    false,
                    mappings
                )
        )
                .isInstanceOf(PaymentsDisabledException.class)
                .hasMessage("Envelope contains payment(s) that are not allowed for jurisdiction 'SSCS', poBox: 'POBOX'");
    }

    @Test
    void assertPaymentsEnabledForContainerIfPaymentsArePresent_should_throw_if_there_are_payments_and_payments_disabled_for_jurisdiction() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
                "SSCS",
                "POBOX",
                SUPPLEMENTARY_EVIDENCE,
                emptyList(),
                singletonList(new InputPayment("dcn1"))
        );

        final List<ContainerMappings.Mapping> mappings =
                singletonList(
                        new ContainerMappings.Mapping("sscs", "SSCS", singletonList("POBOX"), "http://url", false, true)
                );

        // when
        // then
        assertThatThrownBy(
            () ->
                envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                        envelope,
                        true,
                        mappings
                )
        )
            .isInstanceOf(PaymentsDisabledException.class)
            .hasMessage("Envelope contains payment(s) that are not allowed for jurisdiction 'SSCS', poBox: 'POBOX'");
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertDoesNotThrow(() ->
                    envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                            mappings,
                            envelope,
                            CONTAINER
                    )
        );
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertDoesNotThrow(() ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                        mappings,
                        envelope,
                        CONTAINER
                )
        );
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertThatThrownBy(
            () ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    mappings,
                    envelope,
                    CONTAINER
                )
        )
                .isInstanceOf(ContainerJurisdictionPoBoxMismatchException.class)
                .hasMessage("Container, PO Box and jurisdiction mismatch. Jurisdiction: jurisdiction, PO Box: SAMPLE PO BOX 2, container: container");
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertThatThrownBy(
            () ->
                envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(
                    mappings,
                    envelope,
                    CONTAINER
                )
        )
                .isInstanceOf(ContainerJurisdictionPoBoxMismatchException.class)
                .hasMessage("Container, PO Box and jurisdiction mismatch. Jurisdiction: wrong, PO Box: SAMPLE PO BOX 1, container: container");
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertDoesNotThrow(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        mappings
                )
        );
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertDoesNotThrow(() ->
                envelopeValidator.assertServiceEnabled(
                        envelope,
                        mappings
                )
        );
    }

    @Test
    void assertEnvelopeContainsDocsOfAllowedTypesForService_should_pass() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
            "BULKSCAN",
            "POBOX",
            Classification.EXCEPTION,
            List.of(
                scannableItem("file1", InputDocumentType.CHERISHED),
                scannableItem("file2", InputDocumentType.SUPPORTING_DOCUMENTS)
            )
        );

        // when
        // then
        assertDoesNotThrow(() -> envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesForService(envelope));
    }

    @Test
    void assertEnvelopeContainsDocsOfAllowedTypesForService_should_fail() {
        // given
        InputEnvelope envelope = InputEnvelopeCreator.inputEnvelope(
            "BULKSCAN",
            "POBOX",
            Classification.EXCEPTION,
            List.of(
                scannableItem("file1", InputDocumentType.CHERISHED),
                scannableItem("file2", InputDocumentType.SUPPORTING_DOCUMENTS),
                scannableItem("file3", InputDocumentType.IHT),
                scannableItem("file4", InputDocumentType.OTHER)
            )
        );

        // when
        // then
        assertThatThrownBy(() -> envelopeValidator.assertEnvelopeContainsDocsOfAllowedTypesForService(envelope))
            .isInstanceOf(DisallowedDocumentTypesException.class)
            .hasMessage(
                "Envelope contains scannable item(s) of types that are not allowed for jurisdiction 'BULKSCAN': [IHT]"
            );
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

        final List<ContainerMappings.Mapping> mappings = singletonList(m);

        // when
        // then
        assertThatThrownBy(
            () -> envelopeValidator.assertServiceEnabled(
                    envelope,
                    mappings
            )
        )
            .isInstanceOf(ServiceDisabledException.class)
            .hasMessage("Envelope contains service that is not enabled. Jurisdiction: 'jurisdiction' POBox: 'SAMPLE PO BOX 2'");
    }

    private InputOcrData getOcrData() {
        InputOcrData ocrData = new InputOcrData();
        ocrData.setFields(singletonList(new InputOcrDataField(new TextNode("foo"), new TextNode("bar"))));
        return ocrData;
    }
}
