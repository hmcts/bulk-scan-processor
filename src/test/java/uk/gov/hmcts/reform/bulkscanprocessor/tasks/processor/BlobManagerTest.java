package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.CopyState;
import com.microsoft.azure.storage.blob.CopyStatus;
import com.microsoft.azure.storage.blob.LeaseStatus;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.microsoft.azure.storage.StorageErrorCode.LEASE_ID_MISMATCH;
import static com.microsoft.azure.storage.blob.CopyStatus.FAILED;
import static com.microsoft.azure.storage.blob.CopyStatus.PENDING;
import static com.microsoft.azure.storage.blob.CopyStatus.SUCCESS;
import static java.util.Arrays.copyOfRange;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager.LEASE_EXPIRATION_TIME;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
public class BlobManagerTest {

    private static final String INPUT_CONTAINER_NAME = "container-name";
    private static final String REJECTED_CONTAINER_NAME = INPUT_CONTAINER_NAME + "-rejected";
    private static final String INPUT_FILE_NAME = "file-name-123.zip";
    private static final String LEASE_ID = "leaseid123";

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

    @RegisterExtension
    public LogCapturer capturer = LogCapturer.create().captureForType(BlobManager.class);

    @BeforeEach
    public void setUp() throws Exception {
        blobManager = new BlobManager(cloudBlobClient, blobManagementProperties);
    }

    @Test
    public void acquireLease_acquires_lease_on_blob_when_not_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.UNLOCKED);
        given(inputBlob.getProperties()).willReturn(blobProperties);
        given(inputBlob.acquireLease(any(), any())).willReturn(LEASE_ID);

        Optional<String> result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        assertThat(result).isEqualTo(Optional.of(LEASE_ID));
        verify(inputBlob).acquireLease(any(), any());
        verify(inputBlob).uploadMetadata(any(), any(), any());
    }

    @Test
    public void acquireLease_acquires_lease_when_lease_expiration_time_is_before_now() throws Exception {
        // given
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.UNLOCKED);
        given(blobManagementProperties.getBlobLeaseAcquireDelayInSeconds()).willReturn(30);
        given(inputBlob.getProperties()).willReturn(blobProperties);

        HashMap<String, String> metadata = new HashMap<>();
        LocalDateTime initialLeaseExpireTime = LocalDateTime.now(EUROPE_LONDON_ZONE_ID).minusSeconds(60);
        metadata.put(LEASE_EXPIRATION_TIME, initialLeaseExpireTime.toString()); // lease expired
        given(inputBlob.getMetadata()).willReturn(metadata);
        given(inputBlob.acquireLease(any(), any())).willReturn(LEASE_ID);

        // when
        Optional<String> result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        // then
        assertThat(result).isEqualTo(Optional.of(LEASE_ID));
        verify(inputBlob).acquireLease(any(), any());
        verify(inputBlob).uploadMetadata(any(), any(), any());
        String newLeaseAcquiredTime = inputBlob.getMetadata().get(LEASE_EXPIRATION_TIME);
        assertThat(LocalDateTime.parse(newLeaseAcquiredTime))
            .isAfter(initialLeaseExpireTime); // check if metadata is updated after acquiring new lease
    }

    @Test
    public void acquireLease_does_not_acquire_lease_when_already_acquired_lease_is_not_expired()
        throws Exception {
        // given
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.UNLOCKED);
        given(blobManagementProperties.getBlobLeaseAcquireDelayInSeconds()).willReturn(30);
        given(inputBlob.getProperties()).willReturn(blobProperties);
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).plusSeconds(60).toString());
        given(inputBlob.getMetadata()).willReturn(metadata); // lease not expired yet

        // when
        Optional<String> result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        // then
        assertThat(result).isEqualTo(Optional.empty());
        verify(inputBlob, never()).acquireLease(any(), any());
        verify(inputBlob, never()).uploadMetadata();
    }

    @Test
    public void acquireLease_acquires_does_not_acquire_lease_on_blob_when_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.LOCKED);
        given(inputBlob.getProperties()).willReturn(blobProperties);

        Optional<String> result = blobManager.acquireLease(inputBlob, "container-name", "zip-filename.zip");

        assertThat(result).isEqualTo(Optional.empty());
        verify(inputBlob, never()).acquireLease(any(), any());
    }

    @Test
    public void tryReleaseLease_releases_lease_on_blob_and_clears_blob_metadata() throws Exception {
        String leaseId = "lease-id-123";

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(LEASE_EXPIRATION_TIME, LocalDateTime.now(EUROPE_LONDON_ZONE_ID).minusSeconds(10).toString());
        given(inputBlob.getMetadata()).willReturn(metadata);
        blobManager.tryReleaseLease(inputBlob, "container-name", "zip-filename.zip", leaseId);

        ArgumentCaptor<AccessCondition> accessConditionCaptor = ArgumentCaptor.forClass(AccessCondition.class);
        verify(inputBlob).releaseLease(accessConditionCaptor.capture());
        assertThat(accessConditionCaptor.getValue()).isNotNull();
        assertThat(accessConditionCaptor.getValue().getLeaseID()).isEqualTo(leaseId);
        assertThat(inputBlob.getMetadata()).doesNotContainKey(LEASE_EXPIRATION_TIME);
    }

    @Test
    public void tryReleaseLease_does_not_throw_error_when_failure() throws Exception {
        willThrow(new StorageException(LEASE_ID_MISMATCH.toString(), "test exception", null))
            .given(inputBlob)
            .releaseLease(any());

        assertThatCode(
            () ->
                blobManager.tryReleaseLease(
                    inputBlob,
                    "container-name",
                    "zip-filename.zip",
                    "leaseId123"
                )
        ).doesNotThrowAnyException();
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
        given(blobManagementProperties.getBlobSelectedContainer()).willReturn("all");

        List<CloudBlobContainer> containers = blobManager.listInputContainers();
        List<String> containerNames = containers.stream().map(c -> c.getName()).collect(toList());

        assertThat(containerNames).hasSameElementsAs(Arrays.asList("test1", "test2"));
        verify(cloudBlobClient).listContainers();
    }

    @Test
    public void listInputContainers_retrieves_selected_container_from_client() {
        List<CloudBlobContainer> allContainers = Arrays.asList(
            mockContainer("test1"),
            mockContainer("test1-rejected"),
            mockContainer("test2"),
            mockContainer("test3")
        );

        given(cloudBlobClient.listContainers()).willReturn(allContainers);
        given(blobManagementProperties.getBlobSelectedContainer()).willReturn("test2");

        List<CloudBlobContainer> containers = blobManager.listInputContainers();
        List<String> containerNames = containers.stream().map(c -> c.getName()).collect(toList());

        assertThat(containerNames).hasSameElementsAs(Arrays.asList("test2"));
        verify(cloudBlobClient).listContainers();
    }

    @Test
    public void listInputContainers_no_container_found_in_client() {
        List<CloudBlobContainer> allContainers = Arrays.asList(
            mockContainer("test1"),
            mockContainer("test1-rejected"),
            mockContainer("test2")
        );

        given(cloudBlobClient.listContainers()).willReturn(allContainers);
        given(blobManagementProperties.getBlobSelectedContainer()).willReturn("test3");

        List<CloudBlobContainer> containers = blobManager.listInputContainers();

        capturer.assertContains("Container not found for configured container name : test3");

        assertThat(containers).isEmpty();
        verify(cloudBlobClient).listContainers();

    }

    @Test
    public void listRejectedContainers_retrieves_rejected_containers_only() {
        // given
        List<CloudBlobContainer> allContainers = Arrays.asList(
            mockContainer("test1"),
            mockContainer("test1-rejected"),
            mockContainer("test2"),
            mockContainer("test2-rejected")
        );
        given(cloudBlobClient.listContainers()).willReturn(allContainers);

        // when
        List<CloudBlobContainer> rejectedContainers = blobManager.listRejectedContainers();

        // then
        assertThat(rejectedContainers)
            .extracting(c -> c.getName())
            .hasSameElementsAs(Arrays.asList(
                "test1-rejected",
                "test2-rejected"
            ));
    }

    @Test
    public void tryMoveFileToRejectedContainer_copies_and_deletes_original_blob() throws Exception {
        // given
        given(blobManagementProperties.getBlobCopyTimeoutInMillis()).willReturn(1000);
        given(inputContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(inputBlob);
        given(rejectedContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(rejectedBlob);
        given(cloudBlobClient.getContainerReference(INPUT_CONTAINER_NAME)).willReturn(inputContainer);
        given(cloudBlobClient.getContainerReference(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainer);

        // and
        mockRejectedBlobToReturnCopyState(PENDING, SUCCESS);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME, LEASE_ID);

        // then
        verify(rejectedBlob).startCopy(inputBlob);
        verify(inputBlob).deleteIfExists(any(), any(), any(), any());
    }

    @Test
    public void tryMoveFileToRejectedContainer_does_not_delete_blob_when_copying_failed() throws Exception {
        // given
        given(blobManagementProperties.getBlobCopyTimeoutInMillis()).willReturn(1000);
        given(inputContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(inputBlob);
        given(rejectedContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(rejectedBlob);
        given(cloudBlobClient.getContainerReference(INPUT_CONTAINER_NAME)).willReturn(inputContainer);
        given(cloudBlobClient.getContainerReference(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainer);

        // and
        mockRejectedBlobToReturnCopyState(PENDING, PENDING, FAILED);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME, LEASE_ID);

        // then
        verify(rejectedBlob).startCopy(inputBlob);
        verify(rejectedBlob, times(3)).getCopyState();
        verify(inputBlob, never()).deleteIfExists();
    }

    @Test
    public void tryMoveFileToRejectedContainer_does_not_delete_blob_when_copying_timed_out() throws Exception {
        // given
        given(inputContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(inputBlob);
        given(rejectedContainer.getBlockBlobReference(INPUT_FILE_NAME)).willReturn(rejectedBlob);
        given(cloudBlobClient.getContainerReference(INPUT_CONTAINER_NAME)).willReturn(inputContainer);
        given(cloudBlobClient.getContainerReference(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainer);

        // and
        mockRejectedBlobToReturnCopyState(PENDING);

        given(blobManagementProperties.getBlobCopyTimeoutInMillis()).willReturn(300);
        given(blobManagementProperties.getBlobCopyPollingDelayInMillis()).willReturn(100);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME, LEASE_ID);

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
