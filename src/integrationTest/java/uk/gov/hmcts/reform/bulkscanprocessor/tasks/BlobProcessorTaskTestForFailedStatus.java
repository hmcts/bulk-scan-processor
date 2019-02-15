package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;

import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTestForFailedStatus extends ProcessorTestSuite<BlobProcessorTask> {

    @Before
    public void setUp() throws Exception {
        super.setUp(BlobProcessorTask::new);
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_returns_empty_response() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        // and
        given(documentManagementService.uploadDocuments(any())).willReturn(emptyMap());

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).allMatch(item -> item.getDocumentUrl() == null);

        // and
        eventWasCreated(DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_failure_of_upload_once_and_not_reprocess() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        // and
        given(documentManagementService.uploadDocuments(any())).willReturn(emptyMap());

        // when
        processor.processBlobs();

        CloudBlockBlob blob = testContainer.getBlockBlobReference(SAMPLE_ZIP_FILE_NAME);
        await("file should not be deleted")
            .timeout(2, SECONDS)
            .until(blob::exists, is(true));

        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).allMatch(item -> ObjectUtils.isEmpty(item.getDocumentUrl()));

        // and
        eventWasCreated(DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_throws_exception() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        // and
        Throwable throwable = new UnableToUploadDocumentException("oh no", null);
        given(documentManagementService.uploadDocuments(any())).willThrow(throwable);

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).allMatch(e -> ObjectUtils.isEmpty(e.getDocumentUrl()));

        // and
        eventWasCreated(DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_validation_failure_when_zip_does_not_contain_metadata_json() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/missing_metadata"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
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
        eventWasCreated(FILE_VALIDATION_FAILURE);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
    }

    @Test
    public void should_record_signature_failure_when_zip_contains_invalid_signature() throws Exception {
        // given
        processor.signatureAlg = "sha256withrsa";
        processor.publicKeyDerFilename = "signing/test_public_key.der";

        uploadToBlobStorage(
            SAMPLE_ZIP_FILE_NAME,
            zipAndSignDir(
                "zipcontents/ok",
                "signing/some_other_private_key.der" // not matching the public key used for validation!
            )
        );

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventWasCreated(Event.DOC_SIGNATURE_FAILURE);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_SIG_VERIFY_FAILED);
    }

    @Test
    public void should_record_zipfile_processing_event_when_processing_zipfile_fails() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/missing_metadata"));

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();

        assertThat(processEventRepository.findAll())
            .hasOnlyOneElementSatisfying(e -> {
                assertThat(e.getContainer()).isEqualTo(testContainer.getName());
                assertThat(e.getEvent()).isEqualTo(Event.ZIPFILE_PROCESSING_STARTED);
                assertThat(e.getId()).isNotNull();
                assertThat(e.getReason()).isNull();
            });
    }

    private void eventWasCreated(Event event) {
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(2);

        // Verify that Zip file processing event is created
        assertThat(processEvents)
            .filteredOn(e -> e.getEvent().equals(ZIPFILE_PROCESSING_STARTED))
            .hasOnlyOneElementSatisfying(e -> {
                assertThat(e.getContainer()).isEqualTo(testContainer.getName());
                assertThat(e.getEvent()).isEqualTo(event);
                assertThat(e.getId()).isNotNull();
                assertThat(e.getReason()).isNull();
            });

        assertThat(processEvents)
            .filteredOn(e -> e.getEvent().equals(event))
            .hasOnlyOneElementSatisfying(e -> {
                assertThat(e.getContainer()).isEqualTo(testContainer.getName());
                assertThat(e.getEvent()).isEqualTo(event);
                assertThat(e.getId()).isNotNull();
                assertThat(e.getReason()).isNotBlank();
            });
    }

    private void errorWasSent(String zipFileName, ErrorCode code) {
        ArgumentCaptor<ErrorMsg> argument = ArgumentCaptor.forClass(ErrorMsg.class);
        verify(serviceBusHelper).sendMessage(argument.capture());

        ErrorMsg sentMsg = argument.getValue();

        assertThat(sentMsg.zipFileName).isEqualTo(zipFileName);
        assertThat(sentMsg.jurisdiction).isEqualTo(CONTAINER_NAME);
        assertThat(sentMsg.errorCode).isEqualTo(code);
    }

    private void envelopeWasNotCreated() {
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();
    }

}
