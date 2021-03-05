package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumbersInEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipNameNotMatchingMetaDataException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.payment;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;

public class EnvelopeProcessorValidationTest {

    private static final String SAMPLE_URL = "https://example.com/";

    private EnvelopeValidator envelopeValidator;

    @BeforeEach
    public void setUp() {
        envelopeValidator = new EnvelopeValidator();
    }

    @Test
    public void should_throw_exception_when_zip_file_contains_fewer_pdfs() throws Exception {
        // given
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "poBox",
            Classification.EXCEPTION,
            asList(
                scannableItem("hello.pdf"),
                scannableItem("world.pdf")
            )
        );
        List<String> pdfs = singletonList("hello.pdf");

        // when
        Throwable throwable = catchThrowable(() -> envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

        // then
        assertThat(throwable)
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Missing PDFs: world.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_contains_more_pdfs() throws Exception {
        // given
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "poBox",
            Classification.EXCEPTION,
            asList(
                scannableItem("aaa.pdf"),
                scannableItem("bbb.pdf")
            )
        );
        List<String> pdfs = asList(
            "aaa.pdf",
            "bbb.pdf",
            "extra.pdf"
        );

        // when
        Throwable throwable = catchThrowable(() -> envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

        // then
        assertThat(throwable)
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Not declared PDFs: extra.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_has_mismatching_pdf() throws Exception {
        // given
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "poBox",
            Classification.EXCEPTION,
            asList(
                scannableItem("xxx.pdf"),
                scannableItem("yyy.pdf"),
                scannableItem("zzz.pdf")
            )
        );
        List<String> pdfs = asList(
            "xxx.pdf",
            "yyy.pdf",
            "something_not_declared.pdf"
        );

        // when
        Throwable throwable = catchThrowable(() -> envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

        // then
        assertThat(throwable)
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageContaining("Not declared PDFs: something_not_declared.pdf")
            .hasMessageContaining("Missing PDFs: zzz.pdf");
    }

    @Test
    public void should_throw_exception_when_multiple_scannable_items_refer_to_single_pdf() {
        // given
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "poBox",
            Classification.EXCEPTION,
            asList(
                scannableItem("xxx.pdf"),
                scannableItem("yyy.pdf"),
                scannableItem("yyy.pdf")
            )
        );
        List<String> pdfs = asList(
            "xxx.pdf",
            "yyy.pdf"
        );

        // when
        Throwable throwable = catchThrowable(() -> envelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

        // then
        assertThat(throwable)
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessage("Duplicate scanned items file names: yyy.pdf");
    }

    @Test
    public void should_throw_exception_when_document_control_numbers_are_not_unique() {
        // given
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "poBox",
            Classification.EXCEPTION,
            asList(
                scannableItem("1.pdf", "aaa"),
                scannableItem("2.pdf", "bbb"),
                scannableItem("3.pdf", "bbb") // duplicate dcn
            )
        );

        // when
        Throwable throwable = catchThrowable(
            () -> envelopeValidator.assertDocumentControlNumbersAreUnique(envelope)
        );

        // then
        assertThat(throwable)
            .isInstanceOf(DuplicateDocumentControlNumbersInEnvelopeException.class)
            .hasMessage("Duplicate DCNs in envelope: bbb");
    }

    @Test
    public void should_throw_exception_when_required_documents_are_missing() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            "poBox",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.OTHER, new InputOcrData()), // no 'SSCS1' documents
                scannableItem(InputDocumentType.CHERISHED, new InputOcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("No documents");
    }

    @Test
    public void should_throw_exception_when_required_documents_dont_have_ocr() throws Exception {
        SoftAssertions softly = new SoftAssertions();
        Stream.of(InputDocumentType.FORM, InputDocumentType.SSCS1)
            .forEach(type -> {
                InputEnvelope envelope = inputEnvelope(
                    "SSCS",
                    "poBox",
                    Classification.NEW_APPLICATION,
                    asList(
                        scannableItem(type, new InputOcrData())
                    )
                );

                Throwable throwable = catchThrowable(
                    () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
                );

                softly.assertThat(throwable)
                    .as("Expecting exception for doc type " + type)
                    .isInstanceOf(OcrDataNotFoundException.class)
                    .hasMessageContaining("Missing OCR");
            });
        softly.assertAll();
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_not_required() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            "poBox",
            Classification.EXCEPTION, // not NEW_APPLICATION
            asList(
                scannableItem(InputDocumentType.OTHER, new InputOcrData()), // on OCR data
                scannableItem(InputDocumentType.CHERISHED, new InputOcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_not_missing() throws Exception {
        InputOcrData ocrData = new InputOcrData();
        InputOcrDataField field = new InputOcrDataField(new TextNode("name1"), new TextNode("value1"));
        ocrData.setFields(singletonList(field));

        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            "poBox",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.SSCS1, ocrData)
            )
        );

        Throwable throwable = catchThrowable(
            () -> envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_throw_an_exception_when_jurisdiction_poBox_and_container_dont_match() {
        // given
        InputEnvelope envelope = inputEnvelope("test_jurisdiction");
        String container = "container_not_matching_jurisdiction";
        List<Mapping> mappings = emptyList();

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, err);
    }

    @Test
    public void should_throw_an_exception_when_zip_filename_does_not_match_with_metadata() {
        // given
        InputEnvelope envelope = inputEnvelope("test_jurisdiction"); // metadata zip file name "file.zip"

        // when
        Throwable throwable = catchThrowable(
            () -> envelopeValidator.assertZipFilenameMatchesWithMetadata(envelope, "invalid-zip-filename.zip")
        );

        // then
        assertThat(throwable)
            .isInstanceOf(ZipNameNotMatchingMetaDataException.class)
            .hasMessage("Name of the uploaded zip file does not match with field \"zip_file_name\" in the metadata");
    }

    @Test
    public void should_throw_an_exception_when_poBox_doesnt_match_with_jurisdiction_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "abc";
        List<Mapping> mappings = singletonList(new Mapping(container, "ABC", "123", SAMPLE_URL, true, true));

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, err);
    }

    @Test
    public void should_throw_an_exception_when_jurisdiction_doesnt_match_with_poBox_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "test";
        List<Mapping> mappings = singletonList(
            new Mapping(container, "test_jurisdiction", "test_poBox", SAMPLE_URL, true, true)
        );

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, err);
    }

    @Test
    public void should_not_throw_an_exception_when_jurisdiction_poBox_and_container_match() {
        // given
        InputEnvelope envelope = inputEnvelope("Aaa");
        String container = "AaA";
        List<Mapping> mappings = singletonList(
            new Mapping(container, envelope.jurisdiction, envelope.poBox, SAMPLE_URL, true, true)
        );

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        assertThat(err).isNull();
    }

    @Test
    public void should_throw_an_exception_when_payments_present_but_payment_processing_disabled() {
        // given
        InputEnvelope envelope = inputEnvelope(
            "ABC",
            "test_poBox",
            Classification.NEW_APPLICATION,
            emptyList(),
            asList(
                payment("number1")
            )
        );

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                envelope, false, singletonList(new Mapping("abc", "ABC", "test_poBox", null, true, true))
            ));

        // then
        verifyPaymentsDisabledException(envelope, err);
    }

    @Test
    public void should_throw_an_exception_when_payments_present_but_disabled_for_container() {
        // given
        InputEnvelope envelope = inputEnvelope(
            "ABC",
            "test_poBox",
            Classification.NEW_APPLICATION,
            emptyList(),
            asList(
                payment("number1")
            )
        );

        // when
        Throwable err = catchThrowable(
            () -> envelopeValidator.assertPaymentsEnabledForContainerIfPaymentsArePresent(
                envelope, true, singletonList(new Mapping("abc", "ABC", "test_poBox", null, false, true))
            ));

        // then
        verifyPaymentsDisabledException(envelope, err);
    }

    @Test
    public void should_throw_exception_when_required_documents_missing_for_supplementary_evidence_with_ocr() {
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "bulkscanpo",
            Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR,
            asList(
                scannableItem(InputDocumentType.OTHER, new InputOcrData()), // no 'FORM' documents
                scannableItem(InputDocumentType.CHERISHED, new InputOcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("No documents of type Form found");
    }

    @Test
    public void should_throw_exception_when_ocr_data_is_missing_for_supplementary_evidence_with_ocr() {
        InputEnvelope envelope = inputEnvelope(
            "BULKSCAN",
            "bulkscanpo",
            Classification.SUPPLEMENTARY_EVIDENCE_WITH_OCR,
            singletonList(
                scannableItem(InputDocumentType.FORM, new InputOcrData()) // no OCR data
            )
        );

        Throwable throwable = catchThrowable(() ->
            envelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("Missing OCR data");
    }

    @Test
    public void should_throw_exception_for_disabled_service() {
        // given
        InputEnvelope envelope = inputEnvelope("DISABLED_SERVICE", "test_poBox");
        String container = "abc";
        List<Mapping> mappings = singletonList(
            new Mapping(container, "DISABLED_SERVICE", "test_poBox", SAMPLE_URL, true, false)
        );

        // when
        Throwable exception = catchThrowable(
            () -> envelopeValidator.assertServiceEnabled(envelope, mappings)
        );

        // then
        assertThat(exception).isInstanceOf(ServiceDisabledException.class)
            .hasMessageContaining("Envelope contains service that is not enabled");
    }

    private void verifyPaymentsDisabledException(InputEnvelope envelope, Throwable err) {
        assertThat(err)
            .isInstanceOf(PaymentsDisabledException.class)
            .hasMessageContaining("Envelope contains payment(s) that are not allowed for jurisdiction")
            .hasMessageContaining(envelope.jurisdiction)
            .hasMessageContaining(envelope.poBox)
        ;
    }

    private void verifyExceptionIsThrown(InputEnvelope envelope, String container, Throwable err) {
        assertThat(err)
            .isInstanceOf(ContainerJurisdictionPoBoxMismatchException.class)
            .hasMessageContaining(envelope.jurisdiction)
            .hasMessageContaining(envelope.poBox)
            .hasMessageContaining(container);
    }

}
