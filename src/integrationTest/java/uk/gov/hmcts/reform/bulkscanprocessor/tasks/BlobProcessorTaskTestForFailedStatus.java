package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@RunWith(SpringRunner.class)
public class BlobProcessorTaskTestForFailedStatus extends ProcessorTestSuite<BlobProcessorTask> {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        processor = new BlobProcessorTask(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            zipFileProcessor,
            envelopeRepository,
            processEventRepository,
            containerMappings,
            ocrValidator,
            serviceBusHelper,
            paymentsEnabled
        );
    }

    @Test
    public void should_record_validation_failure_when_zip_does_not_contain_metadata_json() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/missing_metadata"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_ZIP_PROCESSING_FAILED);
    }

    @Test
    public void should_record_validation_failure_when_metadata_parsing_fails() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/invalid_metadata"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_metadata_parsing_fails_on_invalid_json_format() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/invalid_json"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_ocr_data_parsing_fails() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/invalid_ocr_data"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_ocr_data_is_missing_for_form_scannable_item() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/missing_ocr_data"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_zip_contains_documents_not_in_pdf_format() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/non_pdf"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_ZIP_PROCESSING_FAILED);
    }

    @Test
    public void should_record_validation_failure_when_jurisdiction_does_not_match_container() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/jurisdiction_mismatching_bulkscan_container"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_reject_file_that_is_not_a_valid_zip_archive() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/ok");
        byte[] corruptedBytes = Arrays.copyOfRange(zipBytes, 1, zipBytes.length - 1);

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, corruptedBytes);

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_ZIP_PROCESSING_FAILED);
    }

    @Test
    public void should_reject_file_which_has_duplicate_cdn_number() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/ok");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // when
        processor.processBlobs();

        // and given
        String filenameForDuplicate = "2_24-06-2018-00-00-00.zip";
        byte[] duplicateZipBytes = zipDir("zipcontents/duplicate_dcn");
        uploadToBlobStorage(filenameForDuplicate, duplicateZipBytes);

        // when
        processor.processBlobs();

        // then
        errorWasSent(filenameForDuplicate, ErrorCode.ERR_ZIP_PROCESSING_FAILED);
        assertThat(envelopeRepository.findAll()).hasSize(1);
    }

    @Test
    public void should_record_validation_failure_when_zip_duplicate_payment_dcns() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/duplicate_payment_dcns"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_zip_missing_required_document_type() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/missing_document")); // no "form" type document

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_error_when_ocr_data_missing_for_supplementary_evidence_with_ocr() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME,
            zipDir("zipcontents/supplementary_evidence_with_ocr_missing_ocr_data")); // no ocr data

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_validation_failure_when_zip_filename_does_not_match_with_metadata() throws Exception {
        // given
        String zipFilename = "1233_24-06-2018-00-00-00.zip";
        // upload metadata with zip_file_name value "1_24-06-2018-00-00-00.zip"
        uploadToBlobStorage(zipFilename, zipDir("zipcontents/ok"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, FILE_VALIDATION_FAILURE);
        fileWasDeleted(zipFilename);
        errorWasSent(zipFilename, ErrorCode.ERR_METAFILE_INVALID);
    }

}
