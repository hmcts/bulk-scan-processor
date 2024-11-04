package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobErrorCode;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.BlobManagementProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.azure.storage.common.implementation.Constants.HeaderConstants.ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BlobManagerTest {

    private static final String INPUT_CONTAINER_NAME = "container-name";
    private static final String REJECTED_CONTAINER_NAME = INPUT_CONTAINER_NAME + "-rejected";
    private static final String INPUT_FILE_NAME = "file-name-123.zip";

    /// start new version
    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient inputContainerClient;

    @Mock
    private BlobContainerClient rejectedContainerClient;

    @Mock
    private BlobClient inputBlobClient;

    @Mock
    private BlobClient rejectedBlobClient;

    @Mock
    private BlobManagementProperties blobManagementProperties;

    private BlobManager blobManager;

    @BeforeEach
    void setUp() {
        blobManager = new BlobManager(blobServiceClient, blobManagementProperties);
    }

    @Test
    void listContainer_retrieves_container_from_client() {
        BlobContainerClient expectedContainer = mock(BlobContainerClient.class);
        String containerName = "container-name";

        given(blobServiceClient.getBlobContainerClient(any())).willReturn(expectedContainer);
        BlobContainerClient actualContainer = blobManager.listContainerClient(containerName);

        assertThat(actualContainer).isSameAs(expectedContainer);
        verify(blobServiceClient).getBlobContainerClient(containerName);
    }

    @Test
    void listInputContainers_retrieves_input_containers_from_client() {
        List<BlobContainerItem> allContainers = Arrays.asList(
            mockBlobContainerItem("test1"),
            mockBlobContainerItem("test1-rejected"),
            mockBlobContainerItem("test2")
        );

        PagedIterable pagedIterable = mock(PagedIterable.class);
        given(blobServiceClient.listBlobContainers()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(allContainers.stream());

        given(blobManagementProperties.getBlobSelectedContainer()).willReturn("all");

        BlobContainerClient test1ContainerClient = mock(BlobContainerClient.class);
        BlobContainerClient test2ContainerClient = mock(BlobContainerClient.class);

        given(blobServiceClient.getBlobContainerClient("test1"))
            .willReturn(test1ContainerClient);
        given(blobServiceClient.getBlobContainerClient("test2"))
            .willReturn(test2ContainerClient);

        List<BlobContainerClient> containers = blobManager.listInputContainerClients();

        assertThat(containers).hasSameElementsAs(Arrays.asList(test1ContainerClient, test2ContainerClient));
        verify(blobServiceClient, times(2)).getBlobContainerClient(anyString());
        verifyNoMoreInteractions(blobServiceClient);
    }

    @Test
    void listRejectedContainers_retrieves_rejected_containers_only() {
        // given
        List<BlobContainerItem> allContainers = Arrays.asList(
            mockBlobContainerItem("test1"),
            mockBlobContainerItem("test1-rejected"),
            mockBlobContainerItem("test2"),
            mockBlobContainerItem("test2-rejected")
        );

        PagedIterable pagedIterable = mock(PagedIterable.class);
        given(blobServiceClient.listBlobContainers()).willReturn(pagedIterable);
        given(pagedIterable.stream()).willReturn(allContainers.stream());
        given(blobServiceClient.getBlobContainerClient(anyString()))
            .willReturn(mock(BlobContainerClient.class));


        // when
        List<BlobContainerClient> rejectedContainers = blobManager.listRejectedContainers();

        // then
        assertThat(rejectedContainers.size()).isEqualTo(2);
        var conditionCapturer = ArgumentCaptor.forClass(String.class);
        verify(blobServiceClient, times(2)).getBlobContainerClient(conditionCapturer.capture());
        assertThat(conditionCapturer.getAllValues())
            .hasSameElementsAs(Arrays.asList(
                "test1-rejected",
                "test2-rejected"
            ));
    }

    @Test
    void tryMoveFileToRejectedContainer_copies_and_deletes_original_blob() {
        // given
        given(inputContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(inputBlobClient);
        given(rejectedContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(rejectedBlobClient);
        given(blobServiceClient.getBlobContainerClient(INPUT_CONTAINER_NAME)).willReturn(inputContainerClient);
        given(blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainerClient);

        // and
        String url = "http://bulk-scan/test.file.txt";
        String sasToken = " 3ewqdeaedfweqwdw";
        mockBeginCopy(url);

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlobClient).beginCopy(any(), any(), any(), any(), any(), any(), any());
        verify(inputBlobClient).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void tryMoveFileToRejectedContainer_does_not_delete_blob_when_beginFromUrl_fails() {
        // given
        given(inputContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(inputBlobClient);
        given(rejectedContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(rejectedBlobClient);
        given(blobServiceClient.getBlobContainerClient(INPUT_CONTAINER_NAME)).willReturn(inputContainerClient);
        given(blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainerClient);

        doThrow(new BlobStorageException("Can not copy", null, null))
            .when(rejectedBlobClient)
            .beginCopy(any(), any(), any(), any(), any(), any(), any());;

        // whenc
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(inputBlobClient, never()).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void tryMoveFileToRejectedContainer_retry_delete_when_lease_lost() {
        // given
        given(inputContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(inputBlobClient);
        given(rejectedContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(rejectedBlobClient);
        given(blobServiceClient.getBlobContainerClient(INPUT_CONTAINER_NAME)).willReturn(inputContainerClient);
        given(blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainerClient);

        given(rejectedBlobClient.exists()).willReturn(Boolean.FALSE);

        mockBeginCopy("http://retry");

        HttpResponse response = mock(HttpResponse.class);
        given(response.getStatusCode()).willReturn(412);
        HttpHeaders httpHeaders =  mock(HttpHeaders.class);
        given(response.getHeaders()).willReturn(httpHeaders);
        given(httpHeaders.getValue(HttpHeaderName.fromString(ERROR_CODE)))
            .willReturn(String.valueOf(BlobErrorCode.LEASE_LOST));


        willThrow(new BlobStorageException(BlobErrorCode.LEASE_LOST.toString(), response, null))
            .willReturn(mock(Response.class))
            .given(inputBlobClient).deleteWithResponse(any(), any(), any(), any());

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlobClient).beginCopy(any(), any(), any(), any(), any(), any(), any());
        verify(inputBlobClient,times(2)).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void tryMoveFileToRejectedContainer_do_not_retry_delete_when_error_different_than_lease_lost() {
        // given
        given(inputContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(inputBlobClient);
        given(rejectedContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(rejectedBlobClient);
        given(blobServiceClient.getBlobContainerClient(INPUT_CONTAINER_NAME)).willReturn(inputContainerClient);
        given(blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainerClient);

        mockBeginCopy("http://test/leaselost");
        given(inputBlobClient.deleteWithResponse(any(), any(), any(), any()))
            .willThrow(new RuntimeException("Does not work"));

        // and

        // when
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlobClient).beginCopy(any(), any(), any(), any(), any(), any(), any());
        verify(inputBlobClient).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void tryMoveFileToRejectedContainer_should_abort_copy_when_begincopy_fails() {

        given(inputContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(inputBlobClient);
        given(rejectedContainerClient.getBlobClient(INPUT_FILE_NAME)).willReturn(rejectedBlobClient);
        given(blobServiceClient.getBlobContainerClient(INPUT_CONTAINER_NAME)).willReturn(inputContainerClient);
        given(blobServiceClient.getBlobContainerClient(REJECTED_CONTAINER_NAME)).willReturn(rejectedContainerClient);

        SyncPoller syncPoller = mock(SyncPoller.class);

        given(rejectedBlobClient
            .beginCopy(any(), any(), any(), any(), any(), any(), any())).willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        willThrow(new RuntimeException("Copy Failed"))
            .given(syncPoller).waitForCompletion(Duration.ofMinutes(5));

        var blobCopyInfo = mock(BlobCopyInfo.class);
        given(syncPoller.poll()).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(blobCopyInfo);

        String copyId = UUID.randomUUID().toString();
        given(blobCopyInfo.getCopyId()).willReturn(copyId);


        // whenc
        blobManager.tryMoveFileToRejectedContainer(INPUT_FILE_NAME, INPUT_CONTAINER_NAME);

        // then
        verify(rejectedBlobClient).beginCopy(any(), any(), any(), any(), any(), any(), any());
        verify(rejectedBlobClient).abortCopyFromUrl(copyId);
        verify(inputBlobClient, never()).deleteWithResponse(any(), any(), any(), any());
    }

    private BlobContainerItem mockBlobContainerItem(String name) {
        BlobContainerItem container = mock(BlobContainerItem.class);
        given(container.getName()).willReturn(name);
        return container;
    }

    private void mockBeginCopy(String url) {
        given(inputBlobClient.getBlobUrl()).willReturn(url);

        SyncPoller syncPoller = mock(SyncPoller.class);
        given(rejectedBlobClient.beginCopy(
            url,
            BlobManager.META_DATA_MAP,
            null,
            null,
            null,
            null,
            Duration.ofSeconds(2)
        )).willReturn(syncPoller);

        var pollResponse = mock(PollResponse.class);
        var blobCopyInfo = mock(BlobCopyInfo.class);

        given(syncPoller.waitForCompletion(Duration.ofMinutes(5))).willReturn(pollResponse);
        given(pollResponse.getValue()).willReturn(mock(BlobCopyInfo.class));

        given(pollResponse.getValue()).willReturn(blobCopyInfo);
    }
}
