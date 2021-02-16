package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.BlobInfo;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@ExtendWith(MockitoExtension.class)
class StaleBlobFinderTest {

    @Mock
    private BlobServiceClient storageClient;

    @Mock
    private ContainerMappings containerMappings;

    @InjectMocks
    private StaleBlobFinder staleBlobFinder;

    @Test
    void should_find_stale_blobs() {
        given(containerMappings.getMappings())
            .willReturn(
                Arrays.asList(getMapping("bulkscan"), getMapping("cmc"), getMapping("sscs"))
            );

        var bulkscanBlobClient = mock(BlobContainerClient.class);
        var cmcBlobClient = mock(BlobContainerClient.class);
        var sscsBlobClient = mock(BlobContainerClient.class);
        given(storageClient.getBlobContainerClient("bulkscan")).willReturn(bulkscanBlobClient);
        given(storageClient.getBlobContainerClient("cmc")).willReturn(cmcBlobClient);
        given(storageClient.getBlobContainerClient("sscs")).willReturn(sscsBlobClient);

        OffsetDateTime creationTime = now().minus(3, ChronoUnit.MINUTES);

        mockStorageList(
            bulkscanBlobClient,
            Stream.of(
                blob("bulk_scan_file_new", false, creationTime),
                blob("bulk_scan_file_stale", true, creationTime)
            )
        );

        mockStorageList(
            cmcBlobClient,
            Stream.of(
                blob("cmc_scan_file_new_1", false, creationTime),
                blob("cmc_scan_file_new_2", false, creationTime)
            )
        );

        mockStorageList(sscsBlobClient, Stream.empty());

        List<BlobInfo> blobInfos = staleBlobFinder.findStaleBlobs(2);

        assertThat(blobInfos.size()).isEqualTo(1);
        assertThat(blobInfos.get(0).container).isEqualTo("bulkscan");
        assertThat(blobInfos.get(0).fileName).isEqualTo("bulk_scan_file_stale");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        var expectedTime =
            dateTimeFormatter.format(ZonedDateTime.ofInstant(creationTime.toInstant(), EUROPE_LONDON_ZONE_ID));

        assertThat(blobInfos.get(0).createdAt).isEqualTo(expectedTime);
    }

    @SuppressWarnings("unchecked")
    private void mockStorageList(BlobContainerClient blobClient, Stream<BlobItem> streamOfBlobItem) {
        PagedIterable<BlobItem> listBlobsResult = mock(PagedIterable.class);
        given(blobClient.listBlobs()).willReturn(listBlobsResult);
        given(listBlobsResult.stream()).willReturn(streamOfBlobItem);

    }

    private BlobItem blob(String name, boolean staleFile, OffsetDateTime creationTime) {
        var blobItem = mock(BlobItem.class);
        var properties = mock(BlobItemProperties.class);

        given(blobItem.getProperties()).willReturn(properties);
        if (staleFile) {
            given(properties.getCreationTime()).willReturn(creationTime);
            given(blobItem.getName()).willReturn(name);
        } else {
            given(properties.getCreationTime()).willReturn(now());
        }
        return blobItem;
    }

    private Mapping getMapping(String containerName) {
        var mapping = new Mapping();
        mapping.setContainer(containerName);
        return mapping;
    }
}
