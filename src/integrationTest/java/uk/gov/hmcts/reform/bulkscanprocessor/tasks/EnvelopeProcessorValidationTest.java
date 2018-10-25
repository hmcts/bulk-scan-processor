package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;

/**
 * This is unit test. Falls under integration to make use of existing zip file resources.
 * TODO: Refactor and move to unit tests. Tested method takes Envelope object and list of PDF objects.
 *       Zip files are not needed.
 */
@RunWith(SpringRunner.class)
public class EnvelopeProcessorValidationTest {

    @Test
    public void should_throw_exception_when_zip_file_contains_fewer_pdfs() throws Exception {
        ZipFileProcessor zipFileProcessor = getZipFileProcessor("zipcontents/fewer_pdfs_than_declared");

        Throwable throwable = catchThrowable(() ->
            EnvelopeProcessor.assertEnvelopeHasPdfs(
                zipFileProcessor.getEnvelope(),
                zipFileProcessor.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Missing PDFs: 1111001.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_contains_more_pdfs() throws Exception {
        ZipFileProcessor zipFileProcessor = getZipFileProcessor("zipcontents/more_pdfs_than_declared");

        Throwable throwable = catchThrowable(() ->
            EnvelopeProcessor.assertEnvelopeHasPdfs(
                zipFileProcessor.getEnvelope(),
                zipFileProcessor.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageMatching("Not declared PDFs: 1111002.pdf");
    }

    @Test
    public void should_throw_exception_when_zip_file_has_mismatching_pdf() throws Exception {
        ZipFileProcessor zipFileProcessor = getZipFileProcessor("zipcontents/mismatching_pdfs");

        Throwable throwable = catchThrowable(() ->
            EnvelopeProcessor.assertEnvelopeHasPdfs(
                zipFileProcessor.getEnvelope(),
                zipFileProcessor.getPdfs()
            )
        );

        assertThat(throwable).isInstanceOf(FileNameIrregularitiesException.class)
            .hasMessageContaining("Not declared PDFs: 1111002.pdf")
            .hasMessageContaining("Missing PDFs: 1111001.pdf");
    }

    private ZipFileProcessor getZipFileProcessor(String zipContentDirectory) throws IOException {
        String container = "container";
        String zipFileName = "hello.zip";

        ZipFileProcessor processor = new ZipFileProcessor();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipDir(zipContentDirectory)))) {
            ZipVerifiers.ZipStreamWithSignature zipWithSignature =
                new ZipVerifiers.ZipStreamWithSignature(zis, null, zipFileName, container);
            processor.process(zipWithSignature, ZipVerifiers.getPreprocessor("none"));
            processor.setEnvelope(EnvelopeCreator.getEnvelopeFromMetafile(processor.getMetadata()));
        }

        return processor;
    }

}
