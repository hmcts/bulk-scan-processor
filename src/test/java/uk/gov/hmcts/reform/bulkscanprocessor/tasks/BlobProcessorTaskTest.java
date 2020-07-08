package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTaskTest {
    @Mock
    private BlobManager blobManager;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private CloudBlobContainer container;

    @Mock
    private CloudBlockBlob cloudBlockBlob;

    @Mock
    private BlobInputStream blobInputStream;

    @Mock
    private FileContentProcessor fileContentProcessor;

    private BlobProcessorTask blobProcessorTask;

    private BlobProcessorTask blobProcessorTaskSpy;

    @BeforeEach
    void setUp() {
        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            envelopeProcessor,
            fileContentProcessor
        );
        blobProcessorTaskSpy = spy(blobProcessorTask);
    }

    @Test
    void processBlobs_should_not_call_envelopeProcessor_if_failed_to_load_file() throws Exception {
        // given
        given(blobManager.listInputContainers()).willReturn(singletonList(container));
        doReturn(singletonList("file.zip")).when(blobProcessorTaskSpy).getFileNames(container);
        given(container.getBlockBlobReference("file.zip")).willReturn(cloudBlockBlob);
        given(container.getName()).willReturn("cont");
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file.zip"))
            .willReturn(null);
        given(cloudBlockBlob.exists()).willReturn(true);
        given(blobManager.acquireLease(any(CloudBlockBlob.class), anyString(), anyString()))
            .willReturn(Optional.of("lease"));
        given(cloudBlockBlob.openInputStream()).willReturn(blobInputStream);
        given(blobInputStream.read(any())).willThrow(new IOException());

        // when
        blobProcessorTaskSpy.processBlobs();

        // then
        verifyNoMoreInteractions(envelopeProcessor);
    }
}
