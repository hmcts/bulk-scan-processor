package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobProcessorTaskTest {
    private BlobProcessorTask blobProcessorTask;

    @Mock
    private BlobManager blobManager;

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository eventRepository;

    @Mock
    private ContainerMappings containerMappings;

    @Mock
    private OcrValidator ocrValidator;

    @Mock
    private ServiceBusHelper notificationsQueueHelper;

    @Mock
    private CloudBlobContainer container;

    @Mock
    private CloudBlockBlob cloudBlockBlob;

    @Mock
    private ListBlobItem blob;

    @Mock
    private BlobProperties blobProperties;

    @Mock
    private Date date;

    @Mock
    private BlobInputStream blobInputStream;

    @BeforeEach
    void setUp() {
        blobProcessorTask = new BlobProcessorTask(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            envelopeRepository,
            eventRepository,
            containerMappings,
            ocrValidator,
            notificationsQueueHelper,
            false
        );
    }

    @Test
    @Disabled
    void processBlobs() throws Exception {
        // given
        given(blobManager.listInputContainers()).willReturn(singletonList(container));
        given(container.listBlobs()).willReturn(singletonList(blob));
        given(blob.getUri()).willReturn(URI.create("file.zip"));
        given(container.getBlockBlobReference("file.zip")).willReturn(cloudBlockBlob);
        given(container.getName()).willReturn("cont");
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file.zip"))
            .willReturn(null);
        given(cloudBlockBlob.exists()).willReturn(true);
        given(cloudBlockBlob.getProperties()).willReturn(blobProperties);
        given(blobProperties.getLastModified()).willReturn(date);
        given(date.before(any(Date.class))).willReturn(true);
        given(blobManager.acquireLease(any(CloudBlockBlob.class), anyString(), anyString()))
            .willReturn(Optional.of("lease"));
        given(cloudBlockBlob.openInputStream()).willReturn(blobInputStream);
        given(blobInputStream.read(any())).willThrow(new IOException());

        // when
        blobProcessorTask.processBlobs();

        // then

    }
}
