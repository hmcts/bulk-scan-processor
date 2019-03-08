package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.INCLUDE_SNAPSHOTS;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CleanUpRejectedFilesTaskTest {

    @Mock private BlobManager blobManager;
    @Mock private CloudBlobContainer container;

    @Before
    public void setUp() throws Exception {
        given(blobManager.listRejectedContainers()).willReturn(singletonList(container));
    }

    @Test
    public void should_remove_only_old_files() throws Exception {
        // given
        Duration deleteDelay = Duration.ofHours(1);

        MockBlob newFile = mockBlob("new.zip", now());
        MockBlob oldFile = mockBlob("old.zip", now().minus(deleteDelay.plusMinutes(1)));

        given(container.listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null))
            .willReturn(asList(
                newFile.listItem,
                oldFile.listItem
            ));

        CleanUpRejectedFilesTask task = new CleanUpRejectedFilesTask(blobManager, deleteDelay);

        // when
        task.run();

        // then
        verify(newFile.blob, times(0)).delete(any(), any(), any(), any());
        verify(oldFile.blob, times(1)).delete(INCLUDE_SNAPSHOTS, null, null, null);
    }

    private MockBlob mockBlob(String fileName, Instant lastModified) throws Exception {
        ListBlobItem listItem = mock(ListBlobItem.class);
        given(listItem.getUri()).willReturn(URI.create(fileName));

        BlobProperties props = mock(BlobProperties.class);
        given(props.getLastModified()).willReturn(Date.from(lastModified));

        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        given(blob.getProperties()).willReturn(props);

        given(container.getBlockBlobReference(fileName)).willReturn(blob);

        return new MockBlob(listItem, blob);
    }

    private class MockBlob {
        protected final ListBlobItem listItem;
        protected final CloudBlockBlob blob;

        protected MockBlob(ListBlobItem listItem, CloudBlockBlob blob) {
            this.listItem = listItem;
            this.blob = blob;
        }
    }
}
