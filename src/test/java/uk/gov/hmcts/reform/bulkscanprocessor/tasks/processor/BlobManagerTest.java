package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.util.Arrays;
import java.util.List;

import static com.microsoft.azure.storage.blob.CopyStatus.FAILED;
import static com.microsoft.azure.storage.blob.CopyStatus.PENDING;
import static com.microsoft.azure.storage.blob.CopyStatus.SUCCESS;
import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BlobManagerTest {

    private static final String INPUT_CONTAINER_NAME = "container-name";
    private static final String REJECTED_CONTAINER_NAME = INPUT_CONTAINER_NAME + "-rejected";
    private static final String INPUT_FILE_NAME = "file-name-123.zip";

    @Mock
    private CloudBlobClient cloudBlobClient;

    @Mock
    private CloudBlockBlob inputBlob;

    @Mock
    private CloudBlockBlob rejectedBlob;

    @Mock
    private BlobProperties blobProperties;

    @Mock
    private BlobManagementProperties blobManagementProperties;

    @Mock
    private CloudBlobContainer inputContainer;

    @Mock
    private CloudBlobContainer rejectedContainer;

    private BlobManager blobManager;

    @Before
    public void setUp() throws Exception {
        given(blobManagementProperties.getBlobLeaseTimeout()).willReturn(30000);
        given(blobManagementProperties.getBlobCopyTimeoutInMillis()).willReturn(1000);
        given(blobManagementProperties.getBlobCopyPollingDelayInMillis()).willReturn(50);

        given(inputContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(inputBlob);
        given(rejectedContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(rejectedBlob);

        given(cloudBlobClient.getContainerReference(INPUT_CONTAINER_NAME)).willReturn(inputContainer);
        given(cloudBlobClient.getContainerReference(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainer);

        blobManager = new BlobManager(cloudBlobClient, blobManagementProperties);
    }

    @Test
    public void acquireLease_acquires_lease_on_blob_when_not_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.UNLOCKED);
        given(inputBlob.getProperties()).willReturn(blobProperties);

        boolean result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        assertThat(result).isTrue();
        verify(inputBlob).acquireLease(any(), any());
    }

    @Test
    public void acquireLease_acquires_does_not_acquire_lease_on_blob_when_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.LOCKED);
        given(inputBlob.getProperties()).willReturn(blobProperties);

        boolean result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        assertThat(result).isFalse();
        verify(inputBlob, never()).acquireLease(any(), any());
    }

    @Test
    public void getContainer_retrieves_container_from_client() throws Exception {
        CloudBlobContainer expectedContainer = mock(CloudBlobContainer.class);
        String containerName = "container-name";

        given(cloudBlobClient.getContainerReference(any())).willReturn(expectedContainer);
        CloudBlobContainer actualContainer = blobManager.getContainer(containerName);

        assertThat(actualContainer).isSameAs(expectedContainer);
        verify(cloudBlobClient).getContainerReference(containerName);
    }

    @Test
    public void listInputContainers_retrieves_input_containers_from_client() {
        List<CloudBlobContainer> allContainers = Arrays.asList(
            mockContainer("test1"),
            mockContainer("test1-rejected"),
            mockContainer("test2")
        );

        given(cloudBlobClient.listContainers()).willReturn(allContainers);
        List<CloudBlobContainer> containers = blobManager.listInputContainers();
        List<String> containerNames = containers.stream().map(c -> c.getName()).collect(toList());

        assertThat(containerNames).hasSameElementsAs(Arrays.asList("test1", "test2"));
        verify(cloudBlobClient).listContainers();
    }

    @Test
    public void tryMoveFileToRejectedContainer_copies_and_deletes_original_blob() throws Exception {
        // given
        mockRejectedBlobToReturnCopyState(PENDING, SUCCESS);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlob).startCopy(inputBlob);
        verify(inputBlob).deleteIfExists();
    }

    @Test
    public void tryMoveFileToRejectedContainer_does_not_delete_blob_when_copying_failed() throws Exception {
        // given
        mockRejectedBlobToReturnCopyState(PENDING, PENDING, FAILED);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlob).startCopy(inputBlob);
        verify(rejectedBlob, times(3)).getCopyState();
        verify(inputBlob, never()).deleteIfExists();
    }

    @Test
    public void tryMoveFileToRejectedContainer_does_not_delete_blob_when_copying_timed_out() throws Exception {
        // given
        mockRejectedBlobToReturnCopyState(PENDING);

        given(blobManagementProperties.getBlobCopyTimeoutInMillis()).willReturn(300);
        given(blobManagementProperties.getBlobCopyPollingDelayInMillis()).willReturn(100);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlob).startCopy(inputBlob);

        int expectedMinInvocations = 2;

        verify(rejectedBlob, atLeast(expectedMinInvocations)).getCopyState();
        verify(inputBlob, never()).deleteIfExists();
    }

    private void mockRejectedBlobToReturnCopyState(CopyStatus... copyStatuses) {
        CopyState[] copyStates =
            Arrays.stream(copyStatuses)
                .map(this::getCopyState)
                .collect(toList())
                .toArray(new CopyState[copyStatuses.length]);

        mockRejectedBlobToReturnCopyState(copyStates);
    }

    private void mockRejectedBlobToReturnCopyState(CopyState... copyStates) {
        if (copyStates.length == 1) {
            given(rejectedBlob.getCopyState()).willReturn(copyStates[0]);
        } else {
            given(rejectedBlob.getCopyState())
                .willReturn(
                    copyStates[0],
                    copyOfRange(copyStates, 1, copyStates.length)
                );
        }
    }

    private CopyState getCopyState(CopyStatus copyStatus) {
        CopyState copyState = mock(CopyState.class);
        given(copyState.getStatus()).willReturn(copyStatus);
        return copyState;
    }

    private CloudBlobContainer mockContainer(String name) {
        CloudBlobContainer container = mock(CloudBlobContainer.class);
        given(container.getName()).willReturn(name);
        return container;
    }
}
