package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;


import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.LeaseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BlobManagerTest {

    @Mock
    private CloudBlobClient cloudBlobClient;

    @Mock
    private CloudBlockBlob blob;

    @Mock
    private BlobProperties blobProperties;

    private BlobManager blobManager;

    @Before
    public void setUp() {
        blobManager = new BlobManager(cloudBlobClient);
    }

    @Test
    public void acquireLease_acquires_lease_on_blob_when_not_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.UNLOCKED);
        given(blob.getProperties()).willReturn(blobProperties);

        boolean result = blobManager.acquireLease(blob, "containername", "zipfilename.zip");

        assertThat(result).isTrue();
        verify(blob).acquireLease(any(), any());
    }

    @Test
    public void acquireLease_acquires_does_not_acquire_lease_on_blob_when_locked() throws Exception {
        given(blobProperties.getLeaseStatus()).willReturn(LeaseStatus.LOCKED);
        given(blob.getProperties()).willReturn(blobProperties);

        boolean result = blobManager.acquireLease(blob, "container-name", "zip-filename.zip");

        assertThat(result).isFalse();
        verify(blob, never()).acquireLease(any(), any());
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
    public void listContainers_retrieves_containers_from_client() {
        List<CloudBlobContainer> expectedContainers =
            Arrays.asList(mock(CloudBlobContainer.class), mock(CloudBlobContainer.class));

        given(cloudBlobClient.listContainers()).willReturn(expectedContainers);
        Iterable<CloudBlobContainer> containers = blobManager.listContainers();

        assertThat(containers).hasSameClassAs(expectedContainers);
        verify(cloudBlobClient).listContainers();
    }
}
