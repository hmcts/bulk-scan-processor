package uk.gov.hmcts.reform.bulkscanprocessor.helper.blobstorage;

import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.ListBlobItem;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class MockBlob {

    public final ListBlobItem listItem;
    public final CloudBlockBlob blob;

    public MockBlob(ListBlobItem listItem, CloudBlockBlob blob) {
        this.listItem = listItem;
        this.blob = blob;
    }

    public static MockBlob mockBlob(
        CloudBlobContainer container,
        String fileName,
        Instant lastModified
    ) throws Exception {
        ListBlobItem listItem = mock(ListBlobItem.class);
        given(listItem.getUri()).willReturn(URI.create(fileName));

        BlobProperties props = mock(BlobProperties.class);
        given(props.getLastModified()).willReturn(Date.from(lastModified));

        CloudBlockBlob blob = mock(CloudBlockBlob.class);
        given(blob.getProperties()).willReturn(props);

        given(container.getBlockBlobReference(fileName)).willReturn(blob);

        return new MockBlob(listItem, blob);
    }
}
