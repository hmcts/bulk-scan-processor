package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.bouncycastle.util.StoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;

import java.util.UUID;
import java.util.function.Consumer;

import static com.azure.storage.blob.models.BlobErrorCode.BLOB_NOT_FOUND;
import static com.azure.storage.blob.models.BlobErrorCode.BLOB_OVERWRITTEN;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD", "unchecked"})
class DeleteFilesServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private EnvelopeMarkAsDeletedService envelopeMarkAsDeletedService;

    @Mock
    private LeaseAcquirer leaseAcquirer;

    @Mock
    private BlobContainerClient container1;

    private DeleteFilesService deleteFilesService;

    private static final String CONTAINER_NAME_1 = "container1";

    @BeforeEach
    void setUp() {
        deleteFilesService = new DeleteFilesService(
            envelopeRepository,
            envelopeMarkAsDeletedService,
            leaseAcquirer
        );
        given(container1.getBlobContainerName()).willReturn(CONTAINER_NAME_1);
    }

    @Test
    void should_delete_single_existing_file() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);

        given(envelopeRepository.getCompleteEnvelopesFromContainer(CONTAINER_NAME_1))
            .willReturn(singletonList(envelope11));
        prepareGivensForEnvelope(container1, blobClient, envelope11);

        leaseCanBeAcquired();

        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);

        verifyBlobClientInteractions(blobClient);
    }

    @Test
    void should_handle_zero_complete_files() {
        // given
        given(envelopeRepository.getCompleteEnvelopesFromContainer(CONTAINER_NAME_1))
            .willReturn(emptyList());

        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_mark_as_deleted_single_non_existing_file() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        final Envelope envelope11 = prepareEnvelope("X", blobClient, container1);
        given(blobClient.exists()).willReturn(false);
        assertThat(envelope11.isZipDeleted()).isFalse();

        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);
        verify(blobClient).exists();
        verifyNoMoreInteractions(blobClient);
    }

    @Test
    void should_handle_not_deleting_existing_file() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        String zipFileName = randomUUID() + ".zip";
        String caseNumber = "12564233455665";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);
        given(envelope.getCaseNumber()).willReturn(caseNumber);

        given(envelopeRepository
            .getCompleteEnvelopesFromContainer(container1.getBlobContainerName()))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobClient.exists()).willReturn(true);

        doThrow(mock(BlobStorageException.class))
            .when(blobClient).deleteWithResponse(any(),any(), any(), any());

        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());


        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);

        verify(blobClient).exists();
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
        verifyNoMoreInteractions(blobClient);
    }

    @Test
    void should_not_delete_file_if_exception_thrown() {
        // given

        String zipFileName = randomUUID() + ".zip";
        String caseNumber = "1714725907404444";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);
        given(envelope.getCaseNumber()).willReturn(caseNumber);

        given(envelopeRepository.getCompleteEnvelopesFromContainer(CONTAINER_NAME_1))
            .willReturn(singletonList(envelope));
        given(container1.getBlobClient(zipFileName))
            .willThrow(new StoreException("msg", new RuntimeException()));

        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);
    }

    @Test
    void should_handle_lease_acquire_exception() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        String zipFileName = randomUUID() + ".zip";

        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);
        given(envelope.getCaseNumber()).willReturn(null);

        given(envelopeRepository
            .getCompleteEnvelopesFromContainer(container1.getBlobContainerName()))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobClient.exists()).willReturn(true);

        doThrow(mock(BlobStorageException.class))
            .when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);
        verify(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());;

        verify(blobClient).exists();
        verify(blobClient, never()).deleteWithResponse(any(), any(), any(), any());
        verifyNoMoreInteractions(blobClient);
    }

    @Test
    void should_mark_as_deleted_when_error_code_blob_not_found() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        String zipFileName = randomUUID() + ".zip";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);

        given(envelopeRepository
            .getCompleteEnvelopesFromContainer(container1.getBlobContainerName()))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobClient.exists()).willReturn(true);


        doAnswer(invocation -> {
            var errorAction = (Consumer) invocation.getArgument(2);
            errorAction.accept(BLOB_NOT_FOUND);
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());


        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyEnvelopesSaving(envelope);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(blobClient);
    }

    @Test
    void should_mark_as_deleted_when_error_code_different_than_blob_not_found() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        String zipFileName = randomUUID() + ".zip";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);

        given(envelopeRepository
            .getCompleteEnvelopesFromContainer(container1.getBlobContainerName()))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobClient.exists()).willReturn(true);


        doAnswer(invocation -> {
            var errorAction = (Consumer) invocation.getArgument(2);
            errorAction.accept(BLOB_OVERWRITTEN);
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());


        // when
        deleteFilesService.processCompleteFiles(container1);

        // then
        verify(envelopeRepository).getCompleteEnvelopesFromContainer(CONTAINER_NAME_1);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(blobClient);
    }

    private Envelope prepareEnvelope(
        String jurisdiction,
        BlobClient blobClient,
        BlobContainerClient container
    ) {
        final Envelope envelope = EnvelopeCreator.envelope(jurisdiction, COMPLETED, container.getBlobContainerName());
        given(envelopeRepository
            .getCompleteEnvelopesFromContainer(container.getBlobContainerName()))
            .willReturn(singletonList(envelope));
        given(container.getBlobClient(envelope.getZipFileName())).willReturn(blobClient);

        return envelope;
    }

    private void prepareGivensForEnvelope(
        BlobContainerClient container,
        BlobClient blobClient,
        Envelope envelope
    ) {
        given(container.getBlobClient(envelope.getZipFileName())).willReturn(blobClient);
        given(blobClient.exists()).willReturn(true);
        given(blobClient.deleteWithResponse(any(), any(), any(), any())).willReturn(mock(Response.class));
        assertThat(envelope.isZipDeleted()).isFalse();
    }

    private void verifyBlobClientInteractions(BlobClient blobClient) {
        verify(blobClient).exists();
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
        verify(blobClient).getBlobUrl();
        verifyNoMoreInteractions(blobClient);
    }

    private void verifyEnvelopesSaving(Envelope... envelopes) {
        ArgumentCaptor<UUID> envelopeIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(envelopeMarkAsDeletedService, times(envelopes.length))
            .markEnvelopeAsDeleted(envelopeIdCaptor.capture(), anyString());
        for (int i = 0; i < envelopes.length; i++) {
            assertThat(envelopeIdCaptor.getAllValues().get(i))
                .isEqualTo(envelopes[i].getId());
        }
    }

    private void leaseCanBeAcquired() {
        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());
    }
}
