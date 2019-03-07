package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(MockitoJUnitRunner.class)
public class EnvelopeProcessorValidationTest {

    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private MetafileJsonValidator schemaValidator;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    @Mock
    private ContainerProperties containerProperties;

    @Before
    public void setUp() {
        envelopeProcessor = new EnvelopeProcessor(
            schemaValidator,
            containerProperties,
            envelopeRepository,
            processEventRepository,
            10,
            10
        );
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
                scannableItem(InputDocumentType.OTHER, new OcrData()), // no 'SSCS1' documents
                scannableItem(InputDocumentType.CHERISHED, new OcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("No documents");
    }

    @Test
    public void should_throw_exception_when_document_type_for_new_applications_is_not_configured_for_given_jurisdiction() throws Exception {
        String invalidJurisdictionForNewApplications = "some_name";

        InputEnvelope envelope = inputEnvelope(
            invalidJurisdictionForNewApplications,
            "poBox",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.OTHER, new OcrData()),
                scannableItem(InputDocumentType.CHERISHED, new OcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining(invalidJurisdictionForNewApplications)
            .hasMessageContaining("not configured");
    }

    @Test
    public void should_throw_exception_when_required_documents_dont_have_ocr() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            "poBox",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.SSCS1, new OcrData()),
                scannableItem(InputDocumentType.SSCS1, new OcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("Missing OCR");
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_not_required() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            "poBox",
            Classification.EXCEPTION, // not NEW_APPLICATION
            asList(
                scannableItem(InputDocumentType.OTHER, new OcrData()), // on OCR data
                scannableItem(InputDocumentType.CHERISHED, new OcrData())
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_not_missing() throws Exception {
        OcrData ocrData = new OcrData();
        OcrDataField field = new OcrDataField(new TextNode("name1"), new TextNode("value1"));
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

        when(containerProperties.getMappings()).thenReturn(emptyList());

        // when
        Throwable err = catchThrowable(() -> envelopeProcessor.assertContainerMatchesJurisdictionAndPoBox(
            envelope.jurisdiction,
            envelope.poBox,
            container
        ));

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_throw_an_exception_when_poBox_doesnt_match_with_jurisdiction_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "abc";

        when(containerProperties.getMappings())
            .thenReturn(singletonList(
                new ContainerProperties.Mapping(container, "ABC", "123")
            ));

        // when
        Throwable err = catchThrowable(() -> envelopeProcessor.assertContainerMatchesJurisdictionAndPoBox(
            envelope.jurisdiction,
            envelope.poBox,
            container
        ));

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_throw_an_exception_when_jurisdiction_doesnt_match_with_poBox_and_container() {
        // given
        InputEnvelope envelope = inputEnvelope("ABC", "test_poBox");
        String container = "test";

        when(containerProperties.getMappings())
            .thenReturn(singletonList(
                new ContainerProperties.Mapping(container, "test_jurisdiction", "test_poBox")
            ));

        // when
        Throwable err = catchThrowable(() -> envelopeProcessor.assertContainerMatchesJurisdictionAndPoBox(
            envelope.jurisdiction,
            envelope.poBox,
            container
        ));

        // then
        verifyExceptionIsThrown(envelope, container, envelope.poBox, err);
    }

    @Test
    public void should_not_throw_an_exception_when_jurisdiction_poBox_and_container_match() {
        // given
        InputEnvelope envelope = inputEnvelope("Aaa");
        String container = "AaA";

        when(containerProperties.getMappings())
            .thenReturn(singletonList(
                new ContainerProperties.Mapping(container, envelope.jurisdiction, envelope.poBox)
            ));

        // when
        Throwable err = catchThrowable(() -> envelopeProcessor.assertContainerMatchesJurisdictionAndPoBox(
            envelope.jurisdiction,
            envelope.poBox,
            container));

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
