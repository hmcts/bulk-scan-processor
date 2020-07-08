package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EligibilityChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
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
    private ListBlobItem blob;

    @Mock
    private BlobInputStream blobInputStream;

    @Mock
    private FileContentProcessor fileContentProcessor;

    @Mock
    private EligibilityChecker eligibilityChecker;

    private BlobProcessorTask blobProcessorTask;

    @BeforeEach
    void setUp() {
        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            envelopeProcessor,
            eligibilityChecker,
            fileContentProcessor
        );
    }

    @Test
    void processBlobs_should_not_call_envelopeProcessor_if_failed_to_load_file() throws Exception {
        // given
        given(blobManager.listInputContainers()).willReturn(singletonList(container));
        given(container.listBlobs()).willReturn(singletonList(blob));
        given(blob.getUri()).willReturn(URI.create("file.zip"));
        given(container.getBlockBlobReference("file.zip")).willReturn(cloudBlockBlob);
        given(container.getName()).willReturn("cont");
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file.zip"))
            .willReturn(null);
        given(eligibilityChecker.isEligibleForProcessing(cloudBlockBlob, "cont", "file.zip")).willReturn(true);
        given(blobManager.acquireLease(any(CloudBlockBlob.class), anyString(), anyString()))
            .willReturn(Optional.of("lease"));
        given(cloudBlockBlob.openInputStream()).willReturn(blobInputStream);
        given(blobInputStream.read(any())).willThrow(new IOException());

        // when
        blobProcessorTask.processBlobs();

        // then
        verifyNoMoreInteractions(envelopeProcessor);
    }
}
