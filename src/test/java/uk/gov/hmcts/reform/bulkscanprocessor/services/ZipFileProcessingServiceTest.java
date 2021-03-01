package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ZipFileProcessingServiceTest {

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private FileContentProcessor fileContentProcessor;

    @Mock
    private LeaseAcquirer leaseAcquirer;

    @Mock
    private OcrValidationRetryManager ocrValidationRetryManager;

    @Mock
    private BlobContainerClient container;

    @Mock
    private BlobItem blob;

    @Mock
    private BlobClient blobClient;

    private ZipFileProcessingService zipFileProcessingService;

    @BeforeEach
    void setUp() {
        zipFileProcessingService = new ZipFileProcessingService(
            envelopeProcessor,
            fileContentProcessor,
            leaseAcquirer,
            ocrValidationRetryManager
        );
    }

    @Test
    void should_not_call_envelopeProcessor_if_failed_to_load_file() throws Exception {
        // given
        given(container.getBlobClient("file.zip")).willReturn(blobClient);
        given(ocrValidationRetryManager.canProcess(blobClient)).willReturn(true);
        given(container.getBlobContainerName()).willReturn("cont");
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file.zip"))
            .willReturn(null);
        given(blobClient.exists()).willReturn(true);

        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        willThrow(new RuntimeException("Can't download")).given(blobClient).openInputStream();

        // when
        zipFileProcessingService.tryProcessZipFile(container, "file.zip");

        // then
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_not_process_file_if_not_ready_to_retry() {
        // given
        given(container.getBlobClient("file.zip")).willReturn(blobClient);
        given(ocrValidationRetryManager.canProcess(blobClient)).willReturn(false);
        given(container.getBlobContainerName()).willReturn("cont");
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file.zip"))
            .willReturn(null);
        given(blobClient.exists()).willReturn(true);

        doAnswer(invocation -> {
            var okAction = (Consumer) invocation.getArgument(1);
            okAction.accept(UUID.randomUUID().toString());
            return null;
        }).when(leaseAcquirer).ifAcquiredOrElse(any(), any(), any(), anyBoolean());

        // when
        zipFileProcessingService.tryProcessZipFile(container, "file.zip");

        // then
        verifyNoMoreInteractions(envelopeProcessor);
        verifyNoMoreInteractions(blobClient);
        verifyNoInteractions(fileContentProcessor);
    }
}
