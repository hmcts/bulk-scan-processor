package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ZipFileProcessingService;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobProcessorTaskTest {
    @Mock
    private BlobManager blobManager;

    @Mock
    private ZipFileProcessingService zipFileProcessingService;

    @Mock
    private BlobContainerClient container;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobItem blob;

    private BlobProcessorTask blobProcessorTask;

    @BeforeEach
    void setUp() {
        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            zipFileProcessingService
        );
    }

    @Test
    void processBlobs_should_call_zipFileProcessingService_if_loaded_file() throws Exception {
        // given
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container));

        PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
        given(container.listBlobs()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(singletonList(blob).stream());

        given(container.getBlobContainerName()).willReturn("cont");
        given(blob.getName()).willReturn("file1.zip");

        // when
        blobProcessorTask.processBlobs();

        // then
        verify(zipFileProcessingService).tryProcessZipFile(container, "file1.zip");
    }

    @Test
    void processBlobs_should_not_call_zipFileProcessingService_if_failed_to_load_file() throws Exception {
        // given
        given(blobManager.listInputContainerClients()).willReturn(singletonList(container));

        PagedIterable<BlobItem> pagedIterable = mock(PagedIterable.class);
        given(container.listBlobs()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(singletonList(blob).stream());

        given(container.getBlobContainerName()).willReturn("cont");

        // when
        blobProcessorTask.processBlobs();

        // then
        verifyNoInteractions(zipFileProcessingService);
    }
}
