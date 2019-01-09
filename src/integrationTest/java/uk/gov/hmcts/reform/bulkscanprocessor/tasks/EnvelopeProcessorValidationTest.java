package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

/**
 * This is unit test. Falls under integration to make use of existing zip file resources.
 * TODO: Refactor and move to unit tests. Tested method takes Envelope object and list of PDF objects.
 * Zip files are not needed.
 */
@RunWith(SpringRunner.class)
public class EnvelopeProcessorValidationTest {

    @Mock
    MetafileJsonValidator schemaValidator;

    @Mock
    EnvelopeRepository envelopeRepository;

    @Mock
    ProcessEventRepository processEventRepository;

    EnvelopeProcessor envelopeProcessor;

    @Before
    public void setup() {
        envelopeProcessor = new EnvelopeProcessor(
            schemaValidator,
            envelopeRepository,
            processEventRepository,
            10,
            10
        );
    }

    @Test
    public void should_throw_exception_when_zip_file_contains_fewer_pdfs() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/fewer_pdfs_than_declared");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            envelopeProcessor.assertEnvelopeHasPdfs(
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
            envelopeProcessor.assertEnvelopeHasPdfs(
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
            envelopeProcessor.assertEnvelopeHasPdfs(
                envelope,
                processingResult.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageContaining("Not declared PDFs: 1111002.pdf")
            .hasMessageContaining("Missing PDFs: 1111001.pdf");
    }

    @Test
    public void should_throw_exception_when_ocr_data_is_missing_for_document_type_sscs1() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/missing_ocr_data");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            envelopeProcessor.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isInstanceOf(OcrDataNotFoundException.class)
            .hasMessageContaining("No scannable items found with ocr data and document type SSCS1");
    }

    @Test
    public void should_not_throw_exception_when_ocr_exists_for_document_type_other() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/ocr_data_not_required");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            envelopeProcessor.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
    }

    @Test
    public void should_not_throw_exception_when_ocr_data_is_missing_for_bulkscan_jurisdiction() throws Exception {
        ZipFileProcessingResult processingResult = processZip("zipcontents/missing_ocr_data_valid");
        InputEnvelope envelope = EnvelopeCreator.getEnvelopeFromMetafile(processingResult.getMetadata());

        Throwable throwable = catchThrowable(() ->
            envelopeProcessor.assertEnvelopeContainsOcrDataIfRequired(envelope)
        );

        assertThat(throwable).isNull();
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
