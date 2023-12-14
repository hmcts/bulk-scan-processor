package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import static com.google.common.io.Resources.getResource;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@Disabled
public class BlobProcessorTaskTest extends ProcessorTestSuite {

    @Test
    public void should_read_blob_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_and_save_metadata_in_database_when_zip_contains_metadata_with_payments_and_pdfs()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/new_application_payments"));
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_and_process_when_zip_contains_metadata_with_payments_for_exception_classification()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/exception_payments"));
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_and_process_when_zip_contains_metadata_with_supplementary_evidence_with_ocr()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/supplementary_evidence_with_ocr"));
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_and_process_when_metadata_contains_supplementary_evidence_with_ocr_and_payments()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/supplementary_evidence_with_ocr_with_payments"));
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_and_save_metadata_in_database_when_zip_contains_rescan_for_value_null()
        throws Exception {
        //Given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/null_rescan_for"));
        testBlobFileProcessed();
    }

    private void testBlobFileProcessed() throws Exception {
        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("zipcontents/ok/metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(CREATED);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                ZIPFILE_PROCESSING_STARTED
            );

        assertThat(processEvents).allMatch(pe -> pe.getReason() == null);
    }

    @Test
    public void should_process_other_zip_files_if_previous_zip_fails_to_process() throws Exception {
        // given
        uploadToBlobStorage("1111_24-06-2018-00-00-00.zip", zipDir("zipcontents/missing_metadata")); //invalid
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok")); //valid

        // when
        processor.processBlobs();

        // then
        // We expect only one envelope from the valid zip file which was uploaded
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("zipcontents/ok/metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
    }

    @Test
    public void should_store_ocr_validation_warnings_in_the_envelope() throws Exception {
        // given
        OcrValidationWarnings ocrValidationWarnings = new OcrValidationWarnings(
            "1111002",
            ImmutableList.of("warning 1", "warning 2")
        );

        given(ocrValidator.assertOcrDataIsValid(any(InputEnvelope.class)))
            .willReturn(Optional.of(ocrValidationWarnings));

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        // when
        processor.processBlobs();

        // then
        Envelope envelope = getSingleEnvelopeFromDb();

        assertThat(envelope.getScannableItems().size()).isEqualTo(1);
        assertThat(envelope.getScannableItems().get(0).getOcrValidationWarnings())
            .hasSameElementsAs(ocrValidationWarnings.warnings);
    }

    @Test
    public void should_not_delete_blob_unless_processed() throws Exception {
        // given
        dbContainsEnvelopeThatWasNotYetDeleted(SAMPLE_ZIP_FILE_NAME, CREATED);

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        // when
        processor.processBlobs();

        // then
        var blob = testContainer.getBlobClient(SAMPLE_ZIP_FILE_NAME);
        await("file should not be deleted")
            .atMost(5, SECONDS)
            .until(blob::exists, is(true));
    }

    private void dbContainsEnvelopeThatWasNotYetDeleted(String zipFileName, Status status) {
        Envelope existingEnvelope = EnvelopeCreator.envelope("A", status);
        existingEnvelope.setZipFileName(zipFileName);
        existingEnvelope.setContainer(testContainer.getBlobContainerName());
        existingEnvelope.setZipDeleted(false);
        envelopeRepository.saveAndFlush(existingEnvelope);
    }
}
