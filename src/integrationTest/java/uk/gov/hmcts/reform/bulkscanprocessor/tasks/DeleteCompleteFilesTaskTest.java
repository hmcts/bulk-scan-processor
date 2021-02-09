package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeJdbcRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@IntegrationTest
@SuppressWarnings({"PMD","unchecked"})
public class DeleteCompleteFilesTaskTest {
    @Mock
    private BlobManager blobManager;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private EnvelopeJdbcRepository envelopeJdbcRepository;

    @Autowired
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private LeaseAcquirer leaseAcquirer;

    private DeleteCompleteFilesTask task;

    @BeforeEach
    public void setUp() {
        this.task = new DeleteCompleteFilesTask(
            blobManager,
            envelopeRepository,
            envelopeJdbcRepository,
            leaseAcquirer
        );
    }

    @Test
    public void should_mark_as_deleted_complete_envelope() {
        // given
        final String containerName1 = "container1";
        final Envelope envelope = envelope("X", COMPLETED, containerName1, false);
        final Envelope envelopeSaved = envelopeRepository.saveAndFlush(envelope);

        final BlobContainerClient container1 = mock(BlobContainerClient.class);
        final BlobClient blobClient = mock(BlobClient.class);
        given(container1.getBlobContainerName()).willReturn(containerName1);
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container1));

        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        given(container1.getBlobClient(envelope.getZipFileName())).willReturn(blobClient);
        given(blobClient.exists()).willReturn(true);
        given(blobClient.deleteWithResponse(any(), any(), any(), any())).willReturn(mock(Response.class));

        // when
        task.run();

        // then
        List<Envelope> envelopesMarkedAsDeleted = envelopeRepository.findByContainerAndStatusAndZipDeleted(
            containerName1,
            COMPLETED,
            true
        );
        assertThat(envelopesMarkedAsDeleted)
            .hasSize(1)
            .extracting(Envelope::getId)
            .containsExactlyInAnyOrder(
                envelopeSaved.getId()
            );
        List<Envelope> envelopesNotMarkedAsDeleted = envelopeRepository.findByContainerAndStatusAndZipDeleted(
            containerName1,
            COMPLETED,
            false
        );
        assertThat(envelopesNotMarkedAsDeleted).isEmpty();
        verify(container1).getBlobClient(envelope.getZipFileName());
        verify(blobClient).exists();
        verify(blobClient).deleteWithResponse(any(), any(), any(), any());

        // and when
        task.run();

        // then
        verify(blobClient).getBlobName();
        verify(blobClient).getContainerName();
        verifyNoMoreInteractions(blobClient);
    }
}
