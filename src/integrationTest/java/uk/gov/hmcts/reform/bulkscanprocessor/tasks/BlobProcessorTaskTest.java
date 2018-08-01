package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocumentNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;


@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTest extends BlobProcessorTestSuite {

    @Test
    public void should_read_from_blob_storage_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        //Given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS); //Zip file with metadata and pdfs

        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf1 = new Pdf("1111001.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf1, pdf2)))
            .willReturn(getFileUploadResponse());

        //when
        blobProcessorTask.processBlobs();

        //then
        //We expect only one envelope which was uploaded
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        String originalMetaFile = Resources.toString(
            getResource("metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(PROCESSED);
        assertThat(actualEnvelope.getScannableItems())
            .extracting("documentUrl")
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_URL1, DOCUMENT_URL2));

        //This verifies pdf file objects were created from the zip file
        verify(documentManagementService).uploadDocuments(ImmutableList.of(pdf1, pdf2));

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents).hasSize(2);

        assertThat(processEvents)
            .extracting("container", "zipFileName", "event")
            .contains(
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_UPLOADED),
                tuple(testContainer.getName(), ZIP_FILE_NAME_SUCCESS, DOC_PROCESSED)
            );

        assertThat(processEvents).extracting("id").hasSize(2);
        assertThat(processEvents).extracting("reason").containsOnlyNulls();
    }

    @Test
    public void should_not_store_documents_in_doc_store_when_zip_does_not_contain_metadata_json() throws Exception {
        //Given
        uploadZipToBlobStore("2_24-06-2018-00-00-00.zip"); //Zip file with only pdfs and no metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);
    }

    @Test
    public void should_process_other_zip_files_if_previous_zip_fails_to_process() throws Exception {
        //Given
        uploadZipToBlobStore("3_24-06-2018-00-00-00.zip"); //Zip with only pdf without metadata
        uploadZipToBlobStore("4_24-06-2018-00-00-00.zip"); //Zip with pdf and metadata

        byte[] test2PdfBytes = toByteArray(getResource("1111002.pdf"));

        Pdf pdf2 = new Pdf("1111002.pdf", test2PdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf2)))
            .willReturn(getFileUploadResponse());

        //when
        blobProcessorTask.processBlobs();

        //then
        //We expect only one envelope 4_24-06-2018-00-00-00.zip which was uploaded
        Envelope actualEnvelope = envelopeRepository.findAll().get(0);

        String originalMetaFile = Resources.toString(
            getResource("metadata_4_24-06-2018-00-00-00.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );

        //This verifies only pdf included in the zip with metadata was processed
        verify(documentManagementService).uploadDocuments(ImmutableList.of(pdf2));

        //Verify first pdf file was never processed
        byte[] test1PdfBytes = toByteArray(getResource("1111001.pdf"));

        verify(documentManagementService, times(0))
            .uploadDocuments(ImmutableList.of(new Pdf("1111001.pdf", test1PdfBytes)));
    }

    @Test
    public void should_not_save_metadata_information_in_db_when_metadata_parsing_fails() throws Exception {
        //Given
        //Invalid deliverydate and openingdate
        uploadZipToBlobStore("6_24-06-2018-00-00-00.zip"); //Zip file with pdf and invalid metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);
    }

    @Test
    public void should_not_save_metadata_information_in_db_when_zip_contains_documents_not_in_pdf_format()
        throws Exception {
        //Given
        uploadZipToBlobStore("5_24-06-2018-00-00-00.zip"); // Zip file with cheque gif and metadata

        //when
        blobProcessorTask.processBlobs();

        //then
        List<Envelope> envelopesInDb = envelopeRepository.findAll();

        assertThat(envelopesInDb).isEmpty();

        verifyZeroInteractions(documentManagementService);
    }

    @Test
    public void should_delete_blob_after_doc_upload_and_mark_envelope_status_as_processed_and_create_new_event()
        throws Exception {
        // Zip with pdf and metadata
        String zipFile = "4_24-06-2018-00-00-00.zip";

        uploadZipToBlobStore(zipFile);

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));
        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(getFileUploadResponse());

        blobProcessorTask.processBlobs();

        //Check blob is deleted
        CloudBlockBlob blob = testContainer.getBlockBlobReference(zipFile);
        await().atMost(2, SECONDS).until(blob::exists, is(false));

        // Verify envelope status is updated to DOC_PROCESSED
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .extracting("status")
            .containsOnly(PROCESSED);

        //Check events created
        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(Collectors.toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOADED, DOC_PROCESSED);
    }

    @Test
    public void should_keep_zip_file_after_unsuccessful_upload_and_not_create_doc_processed_event() throws Exception {
        // Zip with pdf and metadata
        String zipFile = "4_24-06-2018-00-00-00.zip";

        uploadZipToBlobStore(zipFile);

        byte[] testPdfBytes = toByteArray(getResource("1111002.pdf"));
        Pdf pdf = new Pdf("1111002.pdf", testPdfBytes);

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willThrow(DocumentNotFoundException.class);

        blobProcessorTask.processBlobs();

        CloudBlockBlob blob = testContainer.getBlockBlobReference(zipFile);
        await().timeout(2, SECONDS).until(blob::exists, is(true));

        // Verify envelope status is updated to DOC_UPLOAD_FAILED
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .extracting("status")
            .containsOnly(UPLOAD_FAILURE);

        //Check events created
        List<Event> actualEvents = processEventRepository.findAll().stream()
            .map(ProcessEvent::getEvent)
            .collect(Collectors.toList());

        assertThat(actualEvents).containsOnly(DOC_UPLOAD_FAILURE);
    }
}
