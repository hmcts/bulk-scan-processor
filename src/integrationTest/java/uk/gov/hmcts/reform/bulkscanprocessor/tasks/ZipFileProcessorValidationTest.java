package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.rule.OutputCapture;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocUploadFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipInputStream;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * This is unit test. Falls under integration to make use of existing zip file resources.
 */
@RunWith(Parameterized.class)
public class ZipFileProcessorValidationTest {

    @Rule
    public OutputCapture capture = new OutputCapture();

    private static final Processor processor = new Processor(
        null,
        null,
        null,
        new ErrorHandlingWrapper((Throwable t) -> {
            throw new RuntimeException(t);
        })
    ) {
    };

    @Parameterized.Parameters(name = "Processing {0}. Should fail on custom validator? {2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { "1_24-06-2018-00-00-00.zip", 2, false, NullPointerException.class },// valid, can't call dm
            { "2_24-06-2018-00-00-00.zip", 2, false, null },// invalid, zip has folder, no metadata file
            { "3_24-06-2018-00-00-00.zip", 1, false, null },// invalid, no metadata file
            { "4_24-06-2018-00-00-00.zip", 1, true, FileNameIrregularitiesException.class },// incorrect # files
            { "5_24-06-2018-00-00-00.zip", 2, false, null },// contains image
            { "6_24-06-2018-00-00-00.zip", 1, false, null },// invalid metadata file - missing deliverydate
            { "7_24-06-2018-00-00-00.zip", 1, false, NullPointerException.class },// valid, can't call dm
            { "8_24-06-2018-00-00-00.zip", 1, true, FileNameIrregularitiesException.class }// incorrect # files
        });
    }

    private final String zipFileName;

    private final int pdfCount;

    private final boolean whenExpectingMissingPdfs;

    private final Class<? extends Throwable> actualException;

    public ZipFileProcessorValidationTest(
        String zipFileName,
        int pdfCount,
        boolean whenExpectingMissingPdfs,
        Class<? extends Throwable> actualException
    ) {
        this.zipFileName = zipFileName;
        this.pdfCount = pdfCount;
        this.whenExpectingMissingPdfs = whenExpectingMissingPdfs;
        this.actualException = actualException;
    }

    @After
    public void tearDown() {
        capture.flush();
    }

    @Test
    public void should_throw_relevant_exception_when_zip_file_is_invalid() throws IOException, URISyntaxException {
        ZipFileProcessor zipFileProcessor = getZipFileProcessor(zipFileName);

        if (actualException != null) {
            assertThat(capture.toString()).containsPattern(".+ INFO  \\[.+\\] "
                + ZipFileProcessor.class.getCanonicalName() + ":\\d+: "
                + "PDFs found in " + zipFileName + ": " + pdfCount
            );

            Throwable throwable = catchThrowable(() ->
                processor.processParsedEnvelopeDocuments(
                    zipFileProcessor.getEnvelope(),
                    zipFileProcessor.getPdfs(),
                    null
                )
            ).getCause();

            if (whenExpectingMissingPdfs) {
                assertThat(throwable).isInstanceOf(actualException)
                    .hasMessageMatching("Missing PDFs: .+\\.(pdf|jpg|gif).*");
            } else {
                assertThat(throwable).isInstanceOf(DocUploadFailureGenericException.class)
                    .hasCauseExactlyInstanceOf(actualException);
            }
        }
    }

    private ZipFileProcessor getZipFileProcessor(String fileName) throws IOException, URISyntaxException {
        ZipFileProcessor processor = new ZipFileProcessor("container", zipFileName);

        try (ZipInputStream zis = getZipInputStream(zipFileName)) {
            processor.process(zis);
            processor.setEnvelope(EntityParser.parseEnvelopeMetadata(
                new ByteArrayInputStream(processor.getMetadata())
            ));
        } catch (NullPointerException | UnrecognizedPropertyException exception) {
            // nothing to see here:
            // - some zip files does not have metadata
            // - we are forcing json metafile to be invalid
        } catch (NonPdfFileFoundException exception) {
            //Zip '5_24-06-2018-00-00-00.zip' contains non-pdf file: cheque.gif
            assertThat(exception.getMessage()).containsPattern("Zip '"
                + zipFileName + "' contains non-pdf file: "
                + ".+\\.(jpg|gif)"
            );
        }

        return processor;
    }

    private ZipInputStream getZipInputStream(String fileName) throws IOException, URISyntaxException {
        return new ZipInputStream(Files.newInputStream(Paths.get(getResource(fileName).toURI())));
    }
}
