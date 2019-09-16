package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.Arrays;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@RunWith(SpringRunner.class)
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
        assertThat(actualEnvelope.getScannableItems()).allMatch(item -> item.getDocumentUuid() == null);

        // and
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_UPLOAD_FAILURE);
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
        assertThat(actualEnvelope.getScannableItems()).allMatch(item -> ObjectUtils.isEmpty(item.getDocumentUuid()));

        // and
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_UPLOAD_FAILURE);
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
        assertThat(actualEnvelope.getScannableItems()).allMatch(e -> ObjectUtils.isEmpty(e.getDocumentUuid()));

        // and
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_UPLOAD_FAILURE);
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
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_SIGNATURE_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_SIG_VERIFY_FAILED);
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

        // and
        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // and given
        String filenameForDuplicate = "DUP" + SAMPLE_ZIP_FILE_NAME;
        byte[] duplicateZipBytes = zipDir("zipcontents/ok");
        uploadToBlobStorage(filenameForDuplicate, duplicateZipBytes);

        // when
        processor.processBlobs();

        // then
        errorWasSent(filenameForDuplicate, ErrorCode.ERR_ZIP_PROCESSING_FAILED);
        assertThat(envelopeRepository.findAll()).hasSize(1);
    }

    @Test
    public void should_reject_file_which_has_exception_classification_with_payments() throws Exception {
        // given
        byte[] zipBytes = zipDir("zipcontents/exception_payments");

        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipBytes);

        // and
        Pdf pdf = new Pdf(
            "1111002.pdf",
            toByteArray(getResource("zipcontents/exception_payments/1111002.pdf"))
        );

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // then
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_METAFILE_INVALID);
        assertThat(envelopeRepository.findAll()).hasSize(0);
    }

    @Test
    public void should_record_validation_failure_when_zip_very_long_property_value() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/too_long_dcn"));
        // will cause:
        // DataException: could not execute statement
        // PSQLException: ERROR: value too long for type character varying(100)

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_FAILURE);
    }

    private void eventsWereCreated(Event event1, Event event2) {
        assertThat(processEventRepository.findAll())
            .hasSize(2)
            .extracting(e -> tuple(e.getContainer(), e.getEvent()))
            .containsExactlyInAnyOrder(
                tuple(testContainer.getName(), event1),
                tuple(testContainer.getName(), event2)
            );
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

    private void fileWasDeleted(String fileName) throws Exception {
        CloudBlockBlob blob = testContainer.getBlockBlobReference(fileName);
        await("file should be deleted").timeout(2, SECONDS).until(blob::exists, is(false));
    }
}
