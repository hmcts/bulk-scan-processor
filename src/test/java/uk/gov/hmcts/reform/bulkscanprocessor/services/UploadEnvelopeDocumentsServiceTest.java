package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UploadEnvelopeDocumentsServiceTest {

    private static final String CONTAINER_1 = "container-1";
    private static final String ZIP_FILE_NAME = "zip-file-name";

    // used to construct service
    @Mock private BlobManager blobManager;
    @Mock private ZipFileProcessor zipFileProcessor;
    @Mock private DocumentProcessor documentProcessor;
    @Mock private EnvelopeProcessor envelopeProcessor;
    @Mock private LeaseAcquirer leaseAcquirer;

    // used inside the service methods
    @Mock private BlobContainerClient blobContainer;
    @Mock private BlobClient blobClient;

    private UploadEnvelopeDocumentsService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadEnvelopeDocumentsService(
            blobManager,
            zipFileProcessor,
            documentProcessor,
            envelopeProcessor,
            leaseAcquirer
        );
    }

    @Test
    void should_do_nothing_when_blob_manager_fails_to_retrieve_container_representation() {
        // given
        willThrow(new RuntimeException("i failed")).given(blobManager).listContainerClient(CONTAINER_1);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);

        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }



    @Test
    void should_do_nothing_when_failing_to_get_block_blob_reference() {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);

        // and
        willThrow(new RuntimeException("Error getting BlobClient"))
            .given(blobContainer).getBlobClient(ZIP_FILE_NAME);

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_acquire_lease() {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlobContainerName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlobClient(ZIP_FILE_NAME)).willReturn(blobClient);

        doThrow(new RuntimeException("Error in uploading docs"))
            .when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes()); // for storage exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_open_stream() {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlobContainerName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlobClient(ZIP_FILE_NAME)).willReturn(blobClient);

        leaseAcquired();

        // and
        willThrow(new RuntimeException("openInputStream error")).given(blobClient).openInputStream();

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor);
    }

    @Test
    void should_do_nothing_when_failing_to_read_blob_input_stream() throws Exception {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlobContainerName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlobClient(ZIP_FILE_NAME)).willReturn(blobClient);
        leaseAcquired();
        given(blobClient.openInputStream()).willReturn(mock(BlobInputStream.class));

        given(blobClient.getContainerName()).willReturn(CONTAINER_1);
        // and
        willThrow(new IOException("failed")).given(zipFileProcessor)
            .extractPdfFiles(any(ZipInputStream.class), eq(ZIP_FILE_NAME));

        Envelope envelope = mock(Envelope.class);
        UUID envelopeId = UUID.randomUUID();
        given(envelope.getId()).willReturn(envelopeId);
        given(envelope.getZipFileName()).willReturn(ZIP_FILE_NAME);
        // when
        uploadService.processByContainer(CONTAINER_1, singletonList(envelope));

        // then
        verifyNoInteractions(documentProcessor);

        // and
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1))
            .createEvent(eventCaptor.capture(), eq(CONTAINER_1), eq(ZIP_FILE_NAME), eq("failed"), eq(envelopeId));
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOAD_FAILURE);
    }

    @Test
    void should_mark_as_doc_upload_failure_when_unable_to_upload_pdfs() throws Exception {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlobContainerName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlobClient(ZIP_FILE_NAME)).willReturn(blobClient);
        leaseAcquired();
        given(blobClient.openInputStream()).willReturn(mock(BlobInputStream.class));

        given(zipFileProcessor.extractPdfFiles(any(ZipInputStream.class), eq(ZIP_FILE_NAME)))
            .willReturn(emptyList()); // unit test doesn't care if it's empty

        // and
        willThrow(new RuntimeException("oh no")).given(documentProcessor).uploadPdfFiles(emptyList(), emptyList());

        // and

        Envelope envelope = mock(Envelope.class);
        UUID envelopeId = UUID.randomUUID();
        given(envelope.getId()).willReturn(envelopeId);
        given(envelope.getZipFileName()).willReturn(ZIP_FILE_NAME);
        given(envelope.getContainer()).willReturn(CONTAINER_1);

        // when
        uploadService.processByContainer(CONTAINER_1, singletonList(envelope));

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1))
            .createEvent(eventCaptor.capture(), eq(CONTAINER_1), eq(ZIP_FILE_NAME), eq("oh no"), eq(envelopeId));
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOAD_FAILURE);

        // and
        verify(envelopeProcessor, times(1)).markAsUploadFailure(envelope);
    }


    @Test
    void should_mark_as_uploaded_when_everything_went_well() throws Exception {
        // given
        given(blobManager.listContainerClient(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlobContainerName()).willReturn(CONTAINER_1);
        given(blobContainer.getBlobClient(ZIP_FILE_NAME)).willReturn(blobClient);
        leaseAcquired();
        given(blobClient.openInputStream()).willReturn(mock(BlobInputStream.class));

        given(zipFileProcessor.extractPdfFiles(any(ZipInputStream.class), eq(ZIP_FILE_NAME)))
            .willReturn(emptyList()); // unit test doesn't care if it's empty

        // when
        uploadService.processByContainer(CONTAINER_1, getEnvelopes());

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1)).handleEvent(any(Envelope.class), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOADED);

        // and
        verify(documentProcessor, times(1)).uploadPdfFiles(emptyList(), emptyList());

    }

    void leaseAcquired() {
        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

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
            CONTAINER_1,
            null
        ));
    }
}
