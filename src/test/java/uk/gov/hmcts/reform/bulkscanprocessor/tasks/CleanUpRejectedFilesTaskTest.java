package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobContainer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.blobstorage.MockBlob;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.time.Duration;
import java.util.EnumSet;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.INCLUDE_SNAPSHOTS;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.blobstorage.MockBlob.mockBlob;

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
        String ttlString = "PT1H";
        Duration ttl = Duration.parse(ttlString);

        MockBlob newFile = mockBlob(container, "new.zip", now());
        MockBlob oldFile = mockBlob(container, "old.zip", now().minus(ttl.plusMinutes(1)));

        given(container.listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null))
            .willReturn(asList(
                newFile.listItem,
                oldFile.listItem
            ));

        CleanUpRejectedFilesTask task = new CleanUpRejectedFilesTask(blobManager, ttlString);

        // when
        task.run();

        // then
        verify(newFile.blob, times(0)).delete(any(), any(), any(), any());
        verify(oldFile.blob, times(1)).delete(INCLUDE_SNAPSHOTS, null, null, null);
    }
}
