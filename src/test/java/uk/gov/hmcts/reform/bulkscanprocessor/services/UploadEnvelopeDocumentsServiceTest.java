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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;

@ExtendWith(MockitoExtension.class)
class UploadEnvelopeDocumentsServiceTest {

    private static final long MIN_ENVELOPE_AGE_IN_MINUTES = 2;
    private static final String CONTAINER_1 = "container-1";
    private static final String CONTAINER_2 = "container-2";
    private static final String ZIP_FILE_NAME = "zip-file-name";
    private static final String LEASE_ID = "lease-id";

    // used to construct service
    @Mock private EnvelopeRepository envelopeRepository;
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
            MIN_ENVELOPE_AGE_IN_MINUTES,
            envelopeRepository,
            blobManager,
            zipFileProcessor,
            documentProcessor,
            envelopeProcessor
        );
    }

    @Test
    void should_do_nothing_when_no_envelopes_to_process_are_found() {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(emptyList());

        // when
        uploadService.processEnvelopes();

        // then
        verifyNoInteractions(blobManager, zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_do_nothing_when_envelope_is_not_yet_ready_to_be_processed() {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(
            singletonList(getEnvelope(now().plus(MIN_ENVELOPE_AGE_IN_MINUTES, MINUTES)))
        );

        // when
        uploadService.processEnvelopes();

        // then
        verifyNoInteractions(blobManager, zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);
    }

    // will verify grouping by container and throwing different error
    // so both exception branches are covered in a single test
    @Test
    void should_do_nothing_when_failing_to_get_container_client() throws URISyntaxException, StorageException {
        // given
        List<Envelope> envelopes = Arrays.asList(
            getEnvelope(CONTAINER_1),
            getEnvelope(CONTAINER_2)
        );
        given(envelopeRepository.findByStatus(CREATED)).willReturn(envelopes);

        // and
        willThrow(
            new StorageException("error-code", "message", null), // null is inner exception. we don't care here
            new URISyntaxException("input", "reason")
        ).given(blobManager).getContainer(anyString());

        // when
        uploadService.processEnvelopes();

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);

        // and
        verify(blobManager, times(2)).getContainer(anyString());
    }

    @Test
    void should_do_nothing_when_failing_to_get_block_blob_reference() throws URISyntaxException, StorageException {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);

        // and
        willThrow(
            new StorageException("error-code", "message", null), // null is inner exception. we don't care here
            new URISyntaxException("input", "reason")
        ).given(blobContainer).getBlockBlobReference(ZIP_FILE_NAME);

        // when
        uploadService.processEnvelopes(); // for storage exception
        uploadService.processEnvelopes(); // for uri exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_do_nothing_when_failing_to_acquire_lease() throws URISyntaxException, StorageException {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.empty());

        // when
        uploadService.processEnvelopes(); // for storage exception

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_do_nothing_when_failing_to_get_blob_input_stream() throws URISyntaxException, StorageException {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));

        // and
        willThrow(new StorageException("error-code", "message", null)).given(blockBlob).openInputStream();

        // when
        uploadService.processEnvelopes();

        // then
        verifyNoInteractions(zipFileProcessor, documentProcessor, envelopeProcessor);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_do_nothing_when_failing_to_read_blob_input_stream()
        throws URISyntaxException, StorageException, IOException {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);

        // and
        willThrow(new IOException("failed")).given(zipFileProcessor)
            .process(any(ZipInputStream.class), eq(CONTAINER_1), eq(ZIP_FILE_NAME));

        // when
        uploadService.processEnvelopes();

        // then
        verifyNoInteractions(documentProcessor);
        verifyNoMoreInteractions(envelopeRepository);

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
        Envelope envelope = getEnvelope();

        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(envelope));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);
        given(zipFileProcessor.process(any(ZipInputStream.class), eq(CONTAINER_1), eq(ZIP_FILE_NAME)))
            .willReturn(new ZipFileProcessingResult(new byte[]{}, emptyList())); // unit test doesn't care if it's empty

        // and
        willThrow(new RuntimeException("oh no")).given(documentProcessor).uploadPdfFiles(emptyList(), emptyList());

        // and verify envelope doc upload failure count is 0
        assertThat(envelope.getUploadFailureCount()).isEqualTo(0);

        // when
        uploadService.processEnvelopes();

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1))
            .createEvent(eventCaptor.capture(), eq(CONTAINER_1), eq(ZIP_FILE_NAME), eq("oh no"), eq(null));
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOAD_FAILURE);

        // and
        ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository, times(1)).saveAndFlush(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue())
            .satisfies(actualEnvelope -> {
                assertThat(actualEnvelope.getUploadFailureCount()).isEqualTo(1);
                assertThat(actualEnvelope.getStatus()).isEqualTo(Status.UPLOAD_FAILURE);
            });
    }

    @Test
    void should_mark_as_uploaded_when_everything_went_well()
        throws URISyntaxException, StorageException, IOException {
        // given
        given(envelopeRepository.findByStatus(CREATED)).willReturn(singletonList(getEnvelope()));
        given(blobManager.getContainer(CONTAINER_1)).willReturn(blobContainer);
        given(blobContainer.getBlockBlobReference(ZIP_FILE_NAME)).willReturn(blockBlob);
        given(blobManager.acquireLease(blockBlob, CONTAINER_1, ZIP_FILE_NAME)).willReturn(Optional.of(LEASE_ID));
        given(blockBlob.openInputStream()).willReturn(blobInputStream);
        given(zipFileProcessor.process(any(ZipInputStream.class), eq(CONTAINER_1), eq(ZIP_FILE_NAME)))
            .willReturn(new ZipFileProcessingResult(new byte[]{}, emptyList())); // unit test doesn't care if it's empty
        willDoNothing().given(documentProcessor).uploadPdfFiles(emptyList(), emptyList());

        // when
        uploadService.processEnvelopes();

        // then
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(envelopeProcessor, times(1)).handleEvent(any(Envelope.class), eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(Event.DOC_UPLOADED);
    }

    private Envelope getEnvelope(String containerName, Instant createdAt) {
        // service is only interested in status, createdAt, file name and container
        // default state is "CREATED" - that's what we need :+1:
        Envelope envelope = new Envelope(
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
            containerName
        );

        try {
            // we need to update createdAt field but it's not modify-able and private final
            Field createdAtField = envelope.getClass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(envelope, createdAt);

            return envelope;
        } catch (NoSuchFieldException | SecurityException exception) {
            throw new RuntimeException("Could not get field entitled 'createdAt' in Envelope", exception);
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            throw new RuntimeException("Could not modify field 'createdAt' in Envelope", exception);
        }
    }

    private Envelope getEnvelope(String containerName) {
        return getEnvelope(containerName, now().minus(MIN_ENVELOPE_AGE_IN_MINUTES + 1, MINUTES));
    }

    private Envelope getEnvelope(Instant createdAt) {
        return getEnvelope(CONTAINER_1, createdAt);
    }

    private Envelope getEnvelope() {
        return getEnvelope(CONTAINER_1, now().minus(MIN_ENVELOPE_AGE_IN_MINUTES + 1, MINUTES));
    }
}
