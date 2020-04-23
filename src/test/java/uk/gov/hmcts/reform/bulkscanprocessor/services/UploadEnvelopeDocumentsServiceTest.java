package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UploadEnvelopeDocumentsServiceTest {

    private static final String CONTAINER_1 = "container-1";
    private static final String ZIP_FILE_NAME = "zip-file-name";
    private static final String LEASE_ID = "lease-id";

    // used to construct service
    @Mock private BlobManager blobManager;
    @Mock private ZipFileProcessor zipFileProcessor;
    @Mock private DocumentProcessor documentProcessor;
    @Mock private EnvelopeProcessor envelopeProcessor;

    // used inside the service methods
    @Mock private CloudBlobContainer blobContainer;
    @Mock private CloudBlockBlob blockBlob;
    @Mock private BlobInputStream blobInputStream;

    private UploadEnvelopeDocumentsService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadEnvelopeDocumentsService(
            blobManager,
            zipFileProcessor,
            documentProcessor,
            envelopeProcessor
        );
    }

    @Test
    void should_do_nothing_when_unknown_exception_is_caught_in_parent_try_block()
        throws URISyntaxException, StorageException {
        // given
        willThrow(new RuntimeException("i failed")).given(blobManager).getContainer(CONTAINER_1);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_get_container_client() throws URISyntaxException, StorageException {
        // given
        willThrow(
            new StorageException("error-code", "message", null), // null is inner exception. we don't care here
            new URISyntaxException("input", "reason")
        ).given(blobManager).getContainer(CONTAINER_1);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);

        // and
        verify(blobManager, times(1)).getContainer(CONTAINER_1);
    }

    @Test
    void should_do_nothing_when_failing_to_get_block_blob_reference() throws URISyntaxException, StorageException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);

        // and
        willThrow(
            new StorageException("error-code", "message", null), // null is inner exception. we don't care here
            new URISyntaxException("input", "reason")
        ).given(blobContainer).getBlockBlobReference(ZIP_FILE_NAME);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes()); // for storage exception
        uploadService.processByContainer(CONTAINER_1, getEnvelopes()); // for uri exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_acquire_lease() throws URISyntaxException, StorageException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.empty());

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes()); // for storage exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_unknown_exception_is_thrown_during_individual_envelope_processing()
        throws URISyntaxException, StorageException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);

        // and
        willThrow(new RuntimeException("i failed"))
            .given(blobManager)
            .acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes()); // for storage exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_get_blob_input_stream() throws URISyntaxException, StorageException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));

        // and
        willThrow(new StorageException("error-code", "message", null)).given(blockBlob).openInputStream();

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_read_blob_input_stream()
        throws URISyntaxException, StorageException, IOException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);

        // and
        willThrow(new IOException("failed")).given(zipFileProcessor)
            .process(any(ZipInputStream.class), eq(ZIP_FILE_NAME));

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(documentProcessor);

        // and
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1))
            .createEvent(eventCaptor.capture(), eq(CONTAINER_1), eq(ZIP_FILE_NAME), eq("failed"), eq(null));
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOAD_FAILURE);
    }

    @Test
    void should_mark_as_doc_upload_failure_when_unable_to_upload_pdfs()
        throws URISyntaxException, StorageException, IOException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);
        given(zipFileProcessor.process(any(ZipInputStream.class), eq(ZIP_FILE_NAME)))
            .willReturn(new ZipFileProcessingResult(new byte[]{}, emptyList())); // unit test doesn't care if it's empty

        // and
        willThrow(new RuntimeException("oh no")).given(documentProcessor).uploadPdfFiles(emptyList(), emptyList());

        // and
        List<Envelope> envelopes = getEnvelopes();

        // when
        uploadService.processByContainer(CONTAINER_1, envelopes);

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1))
            .createEvent(eventCaptor.capture(), eq(CONTAINER_1), eq(ZIP_FILE_NAME), eq("oh no"), eq(null));
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOAD_FAILURE);

        // and
        verify(envelopeProcessor, times(1)).markAsUploadFailure(envelopes.get(0));
    }

    @Test
    void should_mark_as_uploaded_when_everything_went_well()
        throws URISyntaxException, StorageException, IOException {
        // given
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);
        given(zipFileProcessor.process(any(ZipInputStream.class), eq(ZIP_FILE_NAME)))
            .willReturn(new ZipFileProcessingResult(new byte[]{}, emptyList())); // unit test doesn't care if it's empty

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1)).handleEvent(any(Envelope.class), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOADED);

        // and
        verify(documentProcessor, times(1)).uploadPdfFiles(emptyList(), emptyList());
    }

    private List<Envelope> getEnvelopes() {
        // service is only interested in status, createdAt, file name and container
        // default state is "CREATED" - that's what we need :+1:
        return singletonList(new Envelope(
            "po-box",
            "jurisdiction",
            now(), // delivery date
            now(), // opening date
            now(), // zip file created at (from blob storage)
            ZIP_FILE_NAME,
            "case-number",
            "previous-service-case-reference",
            Classification.EXCEPTION,
            emptyList(),
            emptyList(),
            emptyList(),
            CONTAINER_1
        ));
    }
}
