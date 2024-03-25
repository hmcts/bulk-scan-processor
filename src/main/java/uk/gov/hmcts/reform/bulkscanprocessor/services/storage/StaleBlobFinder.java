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

/**
 * Finds stale blobs in the storage account.
 */
@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class StaleBlobFinder {

    private final BlobServiceClient storageClient;
    private final ContainerMappings containerMappings;

    /**
     * Constructor for the StaleBlobFinder.
     * @param storageClient The storage client
     * @param containerMappings The container mappings
     */
    public StaleBlobFinder(BlobServiceClient storageClient, ContainerMappings containerMappings) {
        this.storageClient = storageClient;
        this.containerMappings = containerMappings;
    }

    /**
     * Finds stale blobs in the storage account.
     * @param staleTime The time after which the blob is considered stale
     * @return List of stale blobs
     */
    public List<BlobInfo> findStaleBlobs(int staleTime) {
        return containerMappings.getMappings()
            .stream()
            .flatMap(c -> findStaleBlobsByContainer(c.getContainer(), staleTime))
            .collect(toList());
    }

    /**
     * Finds stale blobs in the storage account for a given container.
     * @param containerName The container name
     * @param staleTime The time after which the blob is considered stale
     * @return List of stale blobs
     */
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

    /**
     * Checks if the blob is stale.
     * @param blobItem The blob item
     * @param staleTime The time after which the blob is considered stale
     * @return true if the blob is stale, false otherwise
     */
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
