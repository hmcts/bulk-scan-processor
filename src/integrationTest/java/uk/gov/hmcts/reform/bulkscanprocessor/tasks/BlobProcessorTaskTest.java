package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentUrlNotRetrievedException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED_NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.helpers.DirectoryZipper.zipDir;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTest extends ProcessorTestSuite<BlobProcessorTask> {

    protected static final String VALID_SIGNED_ZIP_FILE_WITH_CASE_NUMBER = "42_24-06-2018-00-00-00.test.zip";

    @Before
    public void setUp() throws Exception {
        super.setUp(BlobProcessorTask::new);
    }

    @Test
    public void should_read_blob_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER); //Zip file with metadata and pdfs
        testBlobFileProcessed();
    }

    @Test
    public void should_read_blob_check_signature_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        processor.signatureAlg = "sha256withrsa";
        processor.publicKeyBase64 = getXyzPublicKey64();

        uploadZipToBlobStore(VALID_SIGNED_ZIP_FILE_WITH_CASE_NUMBER); //Signed zip file with metadata and pdfs
        testBlobFileProcessed();
    }

    private void testBlobFileProcessed() throws Exception {
        // given
        Pdf pdf1 = new Pdf("1111001.pdf", toByteArray(getResource("1111001.pdf")));
        Pdf pdf2 = new Pdf("1111002.pdf", toByteArray(getResource("1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf1, pdf2)))
            .willReturn(ImmutableMap.of(
                "1111001.pdf", DOCUMENT_URL1,
                "1111002.pdf", DOCUMENT_URL2
            ));

        doNothing().when(serviceBusHelper).sendMessage(any(Msg.class));

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(NOTIFICATION_SENT);
        assertThat(actualEnvelope.getScannableItems())
            .extracting("documentUrl")
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_URL1, DOCUMENT_URL2));
        assertThat(actualEnvelope.isZipDeleted()).isTrue();

        // This verifies pdf file objects were created from the zip file
        verify(documentManagementService).uploadDocuments(ImmutableList.of(pdf1, pdf2));

        verify(serviceBusHelper, times(1)).sendMessage(any());

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(3);
        assertThat(processEvents)
            .as("All events should be related to our envelope")
            .allMatch(pe ->
                Objects.equals(pe.getContainer(), testContainer.getName())
                    && Objects.equals(pe.getZipFileName(), VALID_ZIP_FILE_WITH_CASE_NUMBER)
            );

        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                DOC_UPLOADED,
                DOC_PROCESSED,
                DOC_PROCESSED_NOTIFICATION_SENT
            );

        assertThat(processEvents).allMatch(pe -> pe.getReason() == null);
    }

    @Test
    public void should_process_other_zip_files_if_previous_zip_fails_to_process() throws Exception {
        // given
        uploadToBlobStorage("bad_24-06-2018-00-00-00.zip", zipDir("zipcontents/missing_metadata"));
        uploadToBlobStorage("good_24-06-2018-00-00-00.zip", zipDir("zipcontents/ok"));

        Pdf okPdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(okPdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

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

        //This verifies only pdf included in the zip with metadata was processed
        verify(documentManagementService).uploadDocuments(ImmutableList.of(okPdf));

        //Verify first pdf file was never processed
        byte[] pdfFromBadZip = toByteArray(getResource("zipcontents/missing_metadata/1111001.pdf"));

        verify(documentManagementService, never())
            .uploadDocuments(ImmutableList.of(new Pdf("1111001.pdf", pdfFromBadZip)));
    }

    @Test
    public void should_delete_blob_after_doc_upload_and_mark_envelope_status_as_processed_and_create_new_event()
        throws Exception {

        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of("1111002.pdf", DOCUMENT_URL2));

        // when
        processor.processBlobs();

        // then
        CloudBlockBlob blob = testContainer.getBlockBlobReference(SAMPLE_ZIP_FILE_NAME);
        await("file should be deleted")
            .atMost(2, SECONDS)
            .until(blob::exists, is(false));

        Envelope envelope = getSingleEnvelopeFromDb();

        assertThat(envelope.getStatus()).isEqualTo(NOTIFICATION_SENT);
        assertThat(envelope.isZipDeleted()).isTrue();

        // Check events created
        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOADED, DOC_PROCESSED, DOC_PROCESSED_NOTIFICATION_SENT);
    }

    @Test
    public void should_keep_zip_file_after_unsuccessful_upload_and_not_create_doc_processed_event() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willThrow(DocumentUrlNotRetrievedException.class);

        // when
        processor.processBlobs();

        // then
        CloudBlockBlob blob = testContainer.getBlockBlobReference(SAMPLE_ZIP_FILE_NAME);
        await("file should not be deleted")
            .timeout(2, SECONDS)
            .until(blob::exists, is(true));

        Envelope envelope = getSingleEnvelopeFromDb();

        assertThat(envelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(envelope.isZipDeleted()).isFalse();

        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_increment_failure_count_when_unable_to_upload_file() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/ok"));

        given(documentManagementService.uploadDocuments(any()))
            .willThrow(UnableToUploadDocumentException.class);

        // when
        processor.processBlobs();

        // then
        Envelope envelope = getSingleEnvelopeFromDb();

        assertThat(envelope.getUploadFailureCount()).isEqualTo(1);
        assertThat(envelope.isZipDeleted()).isFalse();
    }

    @Test
    public void should_not_process_again_if_blob_delete_failed() throws Exception {
        // given

        // Create envelope to simulate existing envelope with 'blob delete failed' status
        Envelope existingEnvelope = EnvelopeCreator.envelope("A", Status.PROCESSED);
        existingEnvelope.setZipFileName(VALID_ZIP_FILE_WITH_CASE_NUMBER);
        existingEnvelope.setContainer(testContainer.getName());
        existingEnvelope.setZipDeleted(false);
        existingEnvelope = envelopeRepository.save(existingEnvelope);
        // Upload blob to process. This should not be uploaded or processed.
        // It should only be deleted from storage and the envelope should be marked
        // as processed.
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER); //Zip file with metadata and pdfs

        // when
        processor.processBlobs();

        // then
        // Check blob was never uploaded
        verify(documentManagementService, never()).uploadDocuments(anyList());

        // Check blob is deleted
        CloudBlockBlob blob = testContainer.getBlockBlobReference(VALID_ZIP_FILE_WITH_CASE_NUMBER);
        await("file should be deleted")
            .atMost(2, SECONDS)
            .until(blob::exists, is(false));

        Envelope envelope = getSingleEnvelopeFromDb();
        assertThat(envelope.isZipDeleted()).isTrue();
    }

    @Test
    public void should_not_delete_blob_unless_processed() throws Exception {
        // given

        // Create envelope to simulate existing envelope with 'blob delete failed' status
        Envelope existingEnvelope = EnvelopeCreator.envelope("A", Status.UPLOADED);
        existingEnvelope.setZipFileName(VALID_ZIP_FILE_WITH_CASE_NUMBER);
        existingEnvelope.setContainer(testContainer.getName());
        existingEnvelope.setZipDeleted(false);
        existingEnvelope = envelopeRepository.save(existingEnvelope);
        // Upload blob to process. This should not be deleted from storage
        // as it still needs processing
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER); //Zip file with metadata and pdfs

        // when
        processor.processBlobs();

        // then
        // Check blob was never uploaded
        verify(documentManagementService, never()).uploadDocuments(anyList());

        // Check blob is not deleted
        CloudBlockBlob blob = testContainer.getBlockBlobReference(VALID_ZIP_FILE_WITH_CASE_NUMBER);
        await("file should not be deleted")
            .atMost(5, SECONDS)
            .until(blob::exists, is(true));
    }

    @After
    public void cleanUp() {
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

}
