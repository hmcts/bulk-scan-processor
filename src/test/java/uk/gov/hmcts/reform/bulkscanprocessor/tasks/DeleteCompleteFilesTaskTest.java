package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.bouncycastle.util.StoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.net.URISyntaxException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.BLOB_DELETE_FAILURE;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class DeleteCompleteFilesTaskTest {

    @Mock
    private BlobManager blobManager;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private CloudBlobContainer container1;

    @Mock
    private ProcessEventRepository eventRepository;

    @Captor
    private ArgumentCaptor<ProcessEvent> processEventCaptor;

    private DeleteCompleteFilesTask deleteCompleteFilesTask;

    private static final String CONTAINER_NAME_1 = "container1";
    private static final String CONTAINER_NAME_2 = "container2";

    @BeforeEach
    void setUp() {
        deleteCompleteFilesTask = new DeleteCompleteFilesTask(
            blobManager,
            envelopeRepository,
            eventRepository
        );
        given(container1.getName()).willReturn(CONTAINER_NAME_1);
    }

    @Test
    void should_delete_single_existing_file() throws Exception {
        // given
        final CloudBlockBlob cloudBlockBlob11 = mock(CloudBlockBlob.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);

        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(singletonList(envelope11));
        prepareGivensForEnvelope(container1, cloudBlockBlob11, envelope11);

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);

        verifyCloudBlockBlobInteractions(cloudBlockBlob11);
    }

    @Test
    void should_handle_zero_complete_files() {
        // given
        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(emptyList());

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_mark_as_deleted_single_non_existing_file() throws Exception {
        // given
        final CloudBlockBlob cloudBlockBlob1 = mock(CloudBlockBlob.class);

        final Envelope envelope11 = prepareEnvelope("X", cloudBlockBlob1, container1);
        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(cloudBlockBlob1.exists()).willReturn(false);
        assertThat(envelope11.isZipDeleted()).isFalse();

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyEnvelopesSaving(envelope11);
        verifyNoMoreInteractions(envelopeRepository);
        verifyNoInteractions(eventRepository);

        verify(cloudBlockBlob1).exists();
        verifyNoMoreInteractions(cloudBlockBlob1);
    }

    @Test
    void should_handle_not_deleting_existing_file() throws Exception {
        // given
        final CloudBlockBlob cloudBlockBlob1 = mock(CloudBlockBlob.class);

        final Envelope envelope11 = prepareEnvelope("X", cloudBlockBlob1, container1);
        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(cloudBlockBlob1.exists()).willReturn(true);
        given(cloudBlockBlob1.deleteIfExists()).willReturn(false);
        assertThat(envelope11.isZipDeleted()).isFalse();

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verifyNoInteractions(eventRepository);

        verify(cloudBlockBlob1).exists();
        verify(cloudBlockBlob1).deleteIfExists();
        verifyNoMoreInteractions(cloudBlockBlob1);
    }

    @Test
    void should_not_delete_file_if_exception_thrown() throws Exception {
        // given
        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);

        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(singletonList(envelope11));
        given(container1.getBlockBlobReference(envelope11.getZipFileName()))
            .willThrow(new StoreException("msg", new RuntimeException()));

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verifyNoMoreInteractions(envelopeRepository);
        verify(eventRepository).saveAndFlush(processEventCaptor.capture());
        assertThat(processEventCaptor.getValue().getContainer()).isEqualTo(envelope11.getContainer());
        assertThat(processEventCaptor.getValue().getZipFileName()).isEqualTo(envelope11.getZipFileName());
        assertThat(processEventCaptor.getValue().getEvent()).isEqualTo(BLOB_DELETE_FAILURE);
        assertThat(processEventCaptor.getValue().getReason()).isEqualTo("msg");
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void should_process_second_container_if_processing_the_first_container_throws() throws Exception {
        // given
        final CloudBlockBlob cloudBlockBlob21 = mock(CloudBlockBlob.class);

        final CloudBlobContainer container2 = mock(CloudBlobContainer.class);
        given(container2.getName()).willReturn(CONTAINER_NAME_2);

        given(blobManager.listInputContainers()).willReturn(asList(container1, container2));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willThrow(new RuntimeException("msg"));

        final Envelope envelope21 = prepareEnvelope("Y", cloudBlockBlob21, container2);

        prepareGivensForEnvelope(container2, cloudBlockBlob21, envelope21);
        assertThat(envelope21.isZipDeleted()).isFalse();

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false);
        verifyEnvelopesSaving(envelope21);
        verifyNoMoreInteractions(envelopeRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void should_delete_multiple_existing_files_in_multiple_containers() throws Exception {
        // given
        final CloudBlobContainer container2 = mock(CloudBlobContainer.class);
        final CloudBlockBlob cloudBlockBlob11 = mock(CloudBlockBlob.class);
        final CloudBlockBlob cloudBlockBlob12 = mock(CloudBlockBlob.class);
        final CloudBlockBlob cloudBlockBlob21 = mock(CloudBlockBlob.class);
        final CloudBlockBlob cloudBlockBlob22 = mock(CloudBlockBlob.class);

        final Envelope envelope11 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);
        final Envelope envelope12 = EnvelopeCreator.envelope("X", COMPLETED, CONTAINER_NAME_1);
        final Envelope envelope21 = EnvelopeCreator.envelope("Y", COMPLETED, CONTAINER_NAME_2);
        final Envelope envelope22 = EnvelopeCreator.envelope("Y", COMPLETED, CONTAINER_NAME_2);

        given(blobManager.listInputContainers()).willReturn(asList(container1, container2));
        given(container2.getName()).willReturn(CONTAINER_NAME_2);
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false))
            .willReturn(asList(envelope11, envelope12));
        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false))
            .willReturn(asList(envelope21, envelope22));
        prepareGivensForEnvelope(container1, cloudBlockBlob11, envelope11);
        prepareGivensForEnvelope(container1, cloudBlockBlob12, envelope12);
        prepareGivensForEnvelope(container2, cloudBlockBlob21, envelope21);
        prepareGivensForEnvelope(container2, cloudBlockBlob22, envelope22);

        // when
        deleteCompleteFilesTask.run();

        // then
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_1, COMPLETED, false);
        verify(envelopeRepository).findByContainerAndStatusAndZipDeleted(CONTAINER_NAME_2, COMPLETED, false);
        verifyEnvelopesSaving(envelope11, envelope12, envelope21, envelope22);
        verifyNoMoreInteractions(envelopeRepository);
        verifyNoInteractions(eventRepository);

        verifyCloudBlockBlobInteractions(cloudBlockBlob11);
        verifyCloudBlockBlobInteractions(cloudBlockBlob12);
        verifyCloudBlockBlobInteractions(cloudBlockBlob21);
        verifyCloudBlockBlobInteractions(cloudBlockBlob22);
    }

    private Envelope prepareEnvelope(
        String jurisdiction,
        CloudBlockBlob cloudBlockBlob,
        CloudBlobContainer container
    ) throws URISyntaxException, StorageException {
        final Envelope envelope = EnvelopeCreator.envelope(jurisdiction, COMPLETED, container.getName());

        given(envelopeRepository.findByContainerAndStatusAndZipDeleted(container.getName(), COMPLETED, false))
            .willReturn(singletonList(envelope));
        given(container.getBlockBlobReference(envelope.getZipFileName())).willReturn(cloudBlockBlob);

        return envelope;
    }

    private void prepareGivensForEnvelope(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        Envelope envelope
    ) throws URISyntaxException, StorageException {
        given(container.getBlockBlobReference(envelope.getZipFileName())).willReturn(cloudBlockBlob);
        given(cloudBlockBlob.exists()).willReturn(true);
        given(cloudBlockBlob.deleteIfExists()).willReturn(true);
        assertThat(envelope.isZipDeleted()).isFalse();
    }

    private void verifyCloudBlockBlobInteractions(CloudBlockBlob cloudBlockBlob11) throws StorageException {
        verify(cloudBlockBlob11).exists();
        verify(cloudBlockBlob11).deleteIfExists();
        verifyNoMoreInteractions(cloudBlockBlob11);
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
}
