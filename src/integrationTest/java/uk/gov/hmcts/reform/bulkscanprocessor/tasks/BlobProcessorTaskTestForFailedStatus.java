package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;

import java.util.Collections;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

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
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER);

        // and
        given(documentManagementService.uploadDocuments(getUploadResources())).willReturn(Collections.emptyMap());

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).allMatch(item -> item.getDocumentUrl() == null);

        // and
        checkFailureEvent(VALID_ZIP_FILE_WITH_CASE_NUMBER, DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_failure_of_upload_once_and_not_reprocess() throws Exception {
        // given
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER);

        // and
        given(documentManagementService.uploadDocuments(getUploadResources())).willReturn(Collections.emptyMap());

        // when
        processor.processBlobs();

        CloudBlockBlob blob = testContainer.getBlockBlobReference(VALID_ZIP_FILE_WITH_CASE_NUMBER);
        await("file should not be deleted")
            .timeout(2, SECONDS)
            .until(blob::exists, is(true));

        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        checkFailureEvent(VALID_ZIP_FILE_WITH_CASE_NUMBER, DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_failure_of_upload_when_document_management_throws_exception() throws Exception {
        // given
        uploadZipToBlobStore(VALID_ZIP_FILE_WITH_CASE_NUMBER);

        // and
        Throwable throwable = new UnableToUploadDocumentException("oh no", null);
        given(documentManagementService.uploadDocuments(getUploadResources())).willThrow(throwable);

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
        assertThat(actualEnvelope.getScannableItems()).extracting("documentUrl").allMatch(ObjectUtils::isEmpty);

        // and
        checkFailureEvent(VALID_ZIP_FILE_WITH_CASE_NUMBER, DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_record_generic_failure_when_zip_does_not_contain_metadata_json() throws Exception {
        // given
        String noMetafileZip = "2_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(noMetafileZip); //Zip file with only pdfs and no metadata

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        checkFailureEvent(noMetafileZip, DOC_FAILURE);
    }

    @Test
    public void should_record_generic_failure_when_metadata_parsing_fails() throws Exception {
        // given
        String invalidMetafileZip = "6_24-06-2018-00-00-00.zip";
        uploadZipToBlobStore(invalidMetafileZip); //Zip file with pdf and invalid metadata

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        checkFailureEvent(invalidMetafileZip, DOC_FAILURE);
    }

    @Test
    public void should_record_generic_failure_when_zip_contains_documents_not_in_pdf_format() throws Exception {
        // given
        uploadZipToBlobStore("5_24-06-2018-00-00-00.zip");

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        checkFailureEvent("5_24-06-2018-00-00-00.zip", Event.DOC_FAILURE);
    }

    @Test
    public void should_record_signature_failure_when_zip_contains_invalid_signature() throws Exception {
        // given
        processor.signatureAlg = "sha256withrsa";
        processor.publicKeyBase64 = getXyzPublicKey64();

        uploadZipToBlobStore("43_24-06-2018-00-00-00.test.zip");

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        checkFailureEvent("43_24-06-2018-00-00-00.test.zip", Event.DOC_SIGNATURE_FAILURE);
    }

    private void checkFailureEvent(String invalidZipFile, Event event) {
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(1);

        ProcessEvent processEvent = processEvents.get(0);

        assertThat(processEvent.getContainer()).isEqualTo(testContainer.getName());
        assertThat(processEvent.getZipFileName()).isEqualTo(invalidZipFile);
        assertThat(processEvent.getEvent()).isEqualTo(event);
        assertThat(processEvent.getId()).isNotNull();
        assertThat(processEvent.getReason()).isNotBlank();
    }

    private void envelopeWasNotCreated() {
        List<Envelope> envelopesInDb = envelopeRepository.findAll();
        assertThat(envelopesInDb).isEmpty();
    }

}
