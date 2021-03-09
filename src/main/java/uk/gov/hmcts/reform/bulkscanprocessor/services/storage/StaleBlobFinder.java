package uk.gov.hmcts.reform.bulkscanprocessor.services.storage;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.BlobInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class StaleBlobFinder {

    private final BlobServiceClient storageClient;
    private final ContainerMappings containerMappings;

    public StaleBlobFinder(BlobServiceClient storageClient, ContainerMappings containerMappings) {
        this.storageClient = storageClient;
        this.containerMappings = containerMappings;
    }

    public List<BlobInfo> findStaleBlobs(int staleTime) {
        return containerMappings.getMappings()
            .stream()
            .flatMap(c -> findStaleBlobsByContainer(c.getContainer(), staleTime))
            .collect(toList());
    }

    private Stream<BlobInfo> findStaleBlobsByContainer(String containerName, int staleTime) {
        return storageClient.getBlobContainerClient(containerName)
            .listBlobs()
            .stream()
            .filter(b -> isStale(b, staleTime))
            .map(blob -> new BlobInfo(
                    containerName,
                    blob.getName(),
                    blob.getProperties().getCreationTime().toInstant()
                )
            );
    }

    private boolean isStale(BlobItem blobItem, int staleTime) {
        return Instant.now().isAfter(
                blobItem
                    .getProperties()
                    .getCreationTime()
                    .toInstant()
                    .plus(staleTime, ChronoUnit.MINUTES)
            );
    }

}
