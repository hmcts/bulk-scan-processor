package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputOcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;

@SuppressWarnings("checkstyle:LineLength")
public class EnvelopeProcessorValidationTest {

    private final static String SAMPLE_URL = "https://example.com/";

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
        List<Pdf> pdfs = singletonList(new Pdf("hello.pdf", null));

        // when
        Throwable throwable = catchThrowable(() -> EnvelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

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
        List<Pdf> pdfs = asList(
            new Pdf("aaa.pdf", null),
            new Pdf("bbb.pdf", null),
            new Pdf("extra.pdf", null)
        );

        // when
        Throwable throwable = catchThrowable(() -> EnvelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

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
        List<Pdf> pdfs = asList(
            new Pdf("xxx.pdf", null),
            new Pdf("yyy.pdf", null),
            new Pdf("something_not_declared.pdf", null)
        );

        // when
        Throwable throwable = catchThrowable(() -> EnvelopeValidator.assertEnvelopeHasPdfs(envelope, pdfs));

        // then
        assertThat(throwable)
            .isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageContaining("Not declared PDFs: something_not_declared.pdf")
            .hasMessageContaining("Missing PDFs: zzz.pdf");
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
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
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
                    () -> EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
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
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
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

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
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
            () -> EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_throw_an_exception_when_poBox_doesnt_match_with_jurisdiction_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "abc";
        List<Mapping> mappings = singletonList(new Mapping(container, "ABC", "123", SAMPLE_URL));

        // when
        Throwable err = catchThrowable(
            () -> EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_throw_an_exception_when_jurisdiction_doesnt_match_with_poBox_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "test";
        List<Mapping> mappings = singletonList(new Mapping(container, "test_jurisdiction", "test_poBox", SAMPLE_URL));

        // when
        Throwable err = catchThrowable(
            () -> EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_not_throw_an_exception_when_jurisdiction_poBox_and_container_match() {
        // given
        InputEnvelope envelope = inputEnvelope("Aaa");
        String container = "AaA";
        List<Mapping> mappings = singletonList(new Mapping(container, envelope.jurisdiction, envelope.poBox, SAMPLE_URL));

        // when
        Throwable err = catchThrowable(
            () -> EnvelopeValidator.assertContainerMatchesJurisdictionAndPoBox(mappings, envelope, container)
        );

        // then
        assertThat(err).isNull();
    }

    private void verifyExceptionIsThrown(InputEnvelope envelope, String container, String poBox, Throwable err) {
        assertThat(err)
            .isInstanceOf(ContainerJurisdictionPoBoxMismatchException.class)
            .hasMessageContaining(envelope.jurisdiction)
            .hasMessageContaining(envelope.poBox)
            .hasMessageContaining(container);
    }

}
