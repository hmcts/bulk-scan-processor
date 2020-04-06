package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@IntegrationTest
@RunWith(SpringRunner.class)
public class DeleteCompleteFilesTaskTest {
    @Mock
    private BlobManager blobManager;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private EnvelopeProcessor envelopeProcessor;

    private DeleteCompleteFilesTask task;

    @Before
    public void setUp() throws Exception {
        this.task = new DeleteCompleteFilesTask(
            blobManager,
            envelopeRepository
        );
    }

    @Test
    public void should_mark_as_deleted_complete_envelope() throws Exception {
        // given
        final String containerName1 = "container1";
        final Envelope envelope = envelope("X", COMPLETED, containerName1, false);
        final Envelope envelopeSaved = envelopeRepository.saveAndFlush(envelope);

        final CloudBlobContainer container1 = mock(CloudBlobContainer.class);
        final CloudBlockBlob cloudBlockBlob = mock(CloudBlockBlob.class);
        given(container1.getName()).willReturn(containerName1);
        given(blobManager.listInputContainers()).willReturn(singletonList(container1));
        given(container1.getBlockBlobReference(envelope.getZipFileName())).willReturn(cloudBlockBlob);
        given(cloudBlockBlob.exists()).willReturn(true);
        given(cloudBlockBlob.deleteIfExists()).willReturn(true);

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
        verify(container1).getBlockBlobReference(envelope.getZipFileName());
        verify(cloudBlockBlob).exists();
        verify(cloudBlockBlob).deleteIfExists();

        // and when
        task.run();

        // then
        verifyNoMoreInteractions(cloudBlockBlob);
    }
}
