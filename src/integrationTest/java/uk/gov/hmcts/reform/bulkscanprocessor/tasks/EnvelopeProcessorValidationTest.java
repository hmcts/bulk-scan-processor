package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.inputEnvelope;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.InputEnvelopeCreator.scannableItem;

/**
 * This is unit test. Falls under integration to make use of existing zip file resources.
 * TODO: Refactor and move to unit tests. Tested method takes Envelope object and list of PDF objects.
 * Zip files are not needed.
 */
@SuppressWarnings("checkstyle:LineLength")
@RunWith(SpringRunner.class)
public class EnvelopeProcessorValidationTest {

    @Test
    public void should_throw_exception_when_zip_file_contains_fewer_pdfs() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/fewer_pdfs_than_declared");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeHasPdfs(
                envelope,
                processingResult.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Missing PDFs: 1111001.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_contains_more_pdfs() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/more_pdfs_than_declared");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeHasPdfs(
                envelope,
                processingResult.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Not declared PDFs: 1111002.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_has_mismatching_pdf() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/mismatching_pdfs");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeHasPdfs(
                envelope,
                processingResult.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageContaining("Not declared PDFs: 1111002.pdf")
            .hasMessageContaining("Missing PDFs: 1111001.pdf");
    }

    @Test
    public void should_throw_exception_when_required_documents_are_missing() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.OTHER, emptyMap()), // no 'SSCS1' documents
                scannableItem(InputDocumentType.CHERISHED, emptyMap())
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
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.OTHER, emptyMap()),
                scannableItem(InputDocumentType.CHERISHED, emptyMap())
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
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.SSCS1, emptyMap()),
                scannableItem(InputDocumentType.SSCS1, emptyMap())
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
            Classification.EXCEPTION, // not NEW_APPLICATION
            asList(
                scannableItem(InputDocumentType.OTHER, emptyMap()), // on OCR data
                scannableItem(InputDocumentType.CHERISHED, emptyMap())
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_not_missing() throws Exception {
        InputEnvelope envelope = inputEnvelope(
            "SSCS",
            Classification.NEW_APPLICATION,
            asList(
                scannableItem(InputDocumentType.SSCS1, ImmutableMap.of("key", "value"))
            )
        );

        Throwable throwable = catchThrowable(() ->
            EnvelopeValidator.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_throw_an_exception_when_jurisdiction_and_container_dont_match() {
        // given
        InputEnvelope envelope = inputEnvelope("A");
        String container = "B";

        // when
        Throwable err = catchThrowable(() -> EnvelopeValidator.assertContainerMatchesJurisdiction(envelope, container));

        // then
        assertThat(err)
            .isInstanceOf(ContainerJurisdictionMismatchException.class)
            .hasMessageContaining(envelope.jurisdiction)
            .hasMessageContaining(container);
    }

    @Test
    public void should_not_throw_an_exception_when_jurisdiction_and_container_match() {
        // given
        InputEnvelope envelope = inputEnvelope("Aaa");
        String container = "AaA";

        // when
        Throwable err = catchThrowable(() -> EnvelopeValidator.assertContainerMatchesJurisdiction(envelope, container));

        // then
        assertThat(err).isNull();
    }

    private ZipFileProcessingResult processZip(String zipContentDirectory)
        throws IOException {
        String container = "container";
        String zipFileName = "hello.zip";

        ZipFileProcessor processor = new ZipFileProcessor();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipDir(zipContentDirectory)))) {
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                new ZipVerifiers.ZipStreamWithSignature(zis, null, zipFileName, container);

            return processor.process(zipWithSignature, ZipVerifiers.getPreprocessor("none"));
        }
    }

}
