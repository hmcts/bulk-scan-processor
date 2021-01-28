package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

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
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD", "unchecked"})
class DeleteCompleteFilesTaskTest {

    @Mock
    private BlobManager blobManager;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private LeaseAcquirer leaseAcquirer;

    @Mock
    private BlobContainerClient container1;

    private DeleteCompleteFilesTask deleteCompleteFilesTask;

    private static final String CONTAINER_NAME_1 = "container1";
    private static final String CONTAINER_NAME_2 = "container2";
    private static final String LEASE_ID_11 = "LEASEID11";
    private static final String LEASE_ID_12 = "LEASEID12";
    private static final String LEASE_ID_21 = "LEASEID21";
    private static final String LEASE_ID_22 = "LEASEID22";

    @BeforeEach
    void setUp() {
        deleteCompleteFilesTask = new DeleteCompleteFilesTask(
            blobManager,
            envelopeRepository,
            leaseAcquirer,
            COMPLETED.name()
        );
        given(container1.getBlobContainerName()).willReturn(CONTAINER_NAME_1);
    }

    @Test
    void should_delete_single_existing_file() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);

        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(singletonList(envelope11));
        prepareGivensForEnvelope(container1, blobClient, envelope11);

        leaseCanBeAcquired();
        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);

        verifyNoMoreInteractions(blobManager);

        verifyBlobClientInteractions(blobClient);
    }

    @Test
    void should_delete_single_existing_file_with_status_notification_sent() {
        // given
        deleteCompleteFilesTask = new DeleteCompleteFilesTask(
            blobManager,
            envelopeRepository,
            leaseAcquirer,
            NOTIFICATION_SENT.name()
        );
        final BlobClient blobClient = mock(BlobClient.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", NOTIFICATION_SENT, CONTAINER_NAME_1);

        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, NOTIFICATION_SENT, false))
            .willReturn(singletonList(envelope11));
        prepareGivensForEnvelope(container1, blobClient, envelope11);

        leaseCanBeAcquired();
        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, NOTIFICATION_SENT, false);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);

        verifyNoMoreInteractions(blobManager);

        verifyBlobClientInteractions(blobClient);
    }

    @Test
    void should_handle_zero_complete_files() {
        // given
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(emptyList());

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_mark_as_deleted_single_non_existing_file() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        final Envelope envelope11 = prepareEnvelope("X", blobClient, container1);
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(blobClient.exists()).willReturn(false);
        assertThat(envelope11.isZipDeleted()).isFalse();

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
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
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);

        given(envelopeRepository
            .findByContainerAndStatusAndZipDeleted(container1.getBlobContainerName(), COMPLETED,
                false))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(blobClient.exists()).willReturn(true);

        doThrow(mock(BlobStorageException.class))
            .when(blobClient).deleteWithResponse(any(),any(), any(), any());

        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());


        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);
        verifyNoMoreInteractions(blobManager);

        verify(blobClient).exists();
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());
        verifyNoMoreInteractions(blobClient);
    }

    @Test
    void should_not_delete_file_if_exception_thrown() {
        // given

        String zipFileName = randomUUID() + ".zip";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);

        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(singletonList(envelope));
        given(container1.getBlobClient(zipFileName))
            .willThrow(new StoreException("msg", new RuntimeException()));

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);
    }

    @Test
    void should_process_second_container_if_processing_the_first_container_throws() {
        // given
        final BlobClient blobClient21 = mock(BlobClient.class);

        final BlobContainerClient container2 = mock(BlobContainerClient.class);
        given(container2.getBlobContainerName()).willReturn(CONTAINER_NAME_2);

        given(blobManager.listInputContainerClients()).willReturn(asList(container1, container2));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willThrow(new RuntimeException("msg"));

        final Envelope envelope21 = prepareEnvelope("Y", blobClient21, container2);

        prepareGivensForEnvelope(container2, blobClient21, envelope21);
        assertThat(envelope21.isZipDeleted()).isFalse();

        leaseCanBeAcquired();

        // then
        deleteCompleteFilesTask.run();

        // then
        verifyNoMoreInteractions(blobManager);

        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false);
        verifyEnvelopesSaving(envelope21);
        verifyNoMoreInteractions(envelopeRepository);
    }

    @Test
    void should_delete_multiple_existing_files_in_multiple_containers() {
        // given
        final BlobContainerClient container2 = mock(BlobContainerClient.class);
        final BlobClient blobClient11 = mock(BlobClient.class);
        final BlobClient blobClient12 = mock(BlobClient.class);
        final BlobClient blobClient21 = mock(BlobClient.class);
        final BlobClient blobClient22 = mock(BlobClient.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);
        final Envelope envelope12 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);
        final Envelope envelope21 = EnvelopeCreator.envelope("Y", COMPLETED, CONTAINER_NAME_2);
        final Envelope envelope22 = EnvelopeCreator.envelope("Y", COMPLETED, CONTAINER_NAME_2);

        given(blobManager.listInputContainerClients()).willReturn(asList(container1, container2));
        given(container2.getBlobContainerName()).willReturn(CONTAINER_NAME_2);
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(asList(envelope11, envelope12));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false))
            .willReturn(asList(envelope21, envelope22));
        prepareGivensForEnvelope(container1, blobClient11, envelope11);
        prepareGivensForEnvelope(container1, blobClient12, envelope12);
        prepareGivensForEnvelope(container2, blobClient21, envelope21);
        prepareGivensForEnvelope(container2, blobClient22, envelope22);

        leaseCanBeAcquired();

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false);
        verifyEnvelopesSaving(envelope11, envelope12, envelope21, envelope22);
        verifyNoMoreInteractions(envelopeRepository);

        verifyBlobClientInteractions(blobClient11);
        verifyBlobClientInteractions(blobClient12);
        verifyBlobClientInteractions(blobClient21);
        verifyBlobClientInteractions(blobClient22);

        verifyNoMoreInteractions(blobManager);
    }

    @Test
    void should_handle_lease_acquire_exception() {
        // given
        final BlobClient blobClient = mock(BlobClient.class);

        String zipFileName = randomUUID() + ".zip";
        Envelope envelope = mock(Envelope.class);
        given(envelope.getZipFileName()).willReturn(zipFileName);

        given(envelopeRepository
            .findByContainerAndStatusAndZipDeleted(container1.getBlobContainerName(), COMPLETED,
                false))
            .willReturn(singletonList(envelope));

        given(container1.getBlobClient(zipFileName)).willReturn(blobClient);

        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));
        given(blobClient.exists()).willReturn(true);

        doThrow(mock(BlobStorageException.class))
            .when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verify(envelope, never()).setZipDeleted(anyBoolean());
        verifyNoMoreInteractions(envelope);
        verify(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());;
        verifyNoMoreInteractions(blobManager);

        verify(blobClient).exists();
        verify(blobClient, never()).deleteWithResponse(any(), any(), any(), any());
        verifyNoMoreInteractions(blobClient);
    }

    private Envelope prepareEnvelope(
        String jurisdiction,
        BlobClient blobClient,
        BlobContainerClient container
    ) {
        final Envelope envelope = EnvelopeCreator.envelope(jurisdiction, COMPLETED, container.getBlobContainerName());
        given(envelopeRepository
            .findByContainerAndStatusAndZipDeleted(container.getBlobContainerName(), COMPLETED, false))
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
        verify(blobClient).getBlobName();
        verify(blobClient).getContainerName();
        verifyNoMoreInteractions(blobClient);
    }

    private void verifyEnvelopesSaving(Envelope... envelopes) {
        ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository, times(envelopes.length)).saveAndFlush(envelopeCaptor.capture());
        for (int i = 0; i < envelopes.length; i++) {
            assertThat(envelopeCaptor.getAllValues().get(i).getId())
                .isEqualTo(envelopes[i].getId());
            assertThat(envelopeCaptor.getAllValues().get(i).getZipFileName())
                .isEqualTo(envelopes[i].getZipFileName());
            assertThat(envelopeCaptor.getAllValues().get(i).isZipDeleted()).isTrue();
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
