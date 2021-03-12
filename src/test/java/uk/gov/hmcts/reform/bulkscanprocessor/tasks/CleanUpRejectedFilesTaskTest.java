package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseMetaDataChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class CleanUpRejectedFilesTaskTest {

    @Mock private BlobManager blobManager;
    @Mock private LeaseMetaDataChecker leaseMetaDataChecker;

    @Mock private BlobContainerClient containerClient;

    @Mock private BlobLeaseClient leaseClient;

    @Mock private PagedIterable<BlobContainerItem> containers;

    private static final String OLD_REJECTED_BLOB = "file2.zip";
    private static final String REJECTED_CONTAINER = "con-rejected";

    @BeforeEach
    public void setUp() throws Exception {
        given(blobManager.listRejectedContainers()).willReturn(singletonList(containerClient));
    }

    @Test
    void should_remove_only_old_files() {

        String ttlString = "PT1H";
        Duration ttl = Duration.parse(ttlString);

        // given
        PagedIterable<BlobItem> rejectedContainerBlobItems = mock(PagedIterable.class);


        given(containerClient.listBlobs(any(), any())).willReturn(rejectedContainerBlobItems);
        given(containerClient.getBlobContainerName()).willReturn(REJECTED_CONTAINER);

        //properties to get LastModified time
        BlobItemProperties oldBlobItemProperties = mock(BlobItemProperties.class);
        BlobItemProperties newBlobItemProperties = mock(BlobItemProperties.class);
        given(oldBlobItemProperties.getLastModified()).willReturn(OffsetDateTime.now().minus(ttl.plusMinutes(1)));
        given(newBlobItemProperties.getLastModified()).willReturn(OffsetDateTime.now());


        BlobItem oldRejectedBlob = mock(BlobItem.class);
        BlobItem newRejectedBlob = mock(BlobItem.class);
        given(oldRejectedBlob.getProperties()).willReturn(oldBlobItemProperties);
        given(newRejectedBlob.getProperties()).willReturn(newBlobItemProperties);

        //old one will be deleted
        given(oldRejectedBlob.getName()).willReturn(OLD_REJECTED_BLOB);

        BlobClient blobClientToDelete =  mock(BlobClient.class);
        given(containerClient.getBlobClient(OLD_REJECTED_BLOB)).willReturn(blobClientToDelete);

        given(blobClientToDelete.getBlobName()).willReturn(OLD_REJECTED_BLOB);
        given(blobClientToDelete.getContainerName()).willReturn(REJECTED_CONTAINER);

        BlobProperties blobItemProperties = mock(BlobProperties.class);
        given(blobItemProperties.getCopyStatus()).willReturn(null);
        given(blobClientToDelete.getProperties()).willReturn(blobItemProperties);

        given(rejectedContainerBlobItems.stream()).willReturn(Stream.of(newRejectedBlob, oldRejectedBlob));

        given(leaseMetaDataChecker.isReadyToUse(blobClientToDelete)).willReturn(true);

        CleanUpRejectedFilesTask task =
            new CleanUpRejectedFilesTask(
                blobManager,
                new LeaseAcquirer(leaseMetaDataChecker),
                ttlString

            );
        // when
        task.run();

        // then

        var conditionCapturer = ArgumentCaptor.forClass(BlobRequestConditions.class);
        verify(blobClientToDelete, times(1))
            .deleteWithResponse(eq(DeleteSnapshotsOptionType.INCLUDE), conditionCapturer.capture(), any(), any());

    }



}
