package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.INCLUDE_SNAPSHOTS;

public class CleanUpRejectedFilesTask {

    private static final Logger log = LoggerFactory.getLogger(CleanUpRejectedFilesTask.class);

    private final BlobManager blobManager;
    private final Duration deleteDelay;

    // region constructor
    public CleanUpRejectedFilesTask(
        BlobManager blobManager,
        Duration deleteDelay
    ) {
        this.blobManager = blobManager;
        this.deleteDelay = deleteDelay;
    }
    // endregion

    public void run() {
        blobManager
            .listRejectedContainers()
            .forEach(container -> {
                log.info("Deleting old rejected files from container {}", container.getName());
                container
                    .listBlobs(null, true, EnumSet.of(SNAPSHOTS), null, null)
                    .forEach(listItem -> {
                        try {
                            CloudBlockBlob blob = container.getBlockBlobReference(
                                FilenameUtils.getName(listItem.getUri().toString())
                            );

                            if (canBeDeleted(blob)) {
                                blob.delete(INCLUDE_SNAPSHOTS, null, null, null);
                                log.info("Deleted rejected file {}", blob.getName());
                            }
                        } catch (URISyntaxException | StorageException exc) {
                            log.error(
                                "Unable to delete rejected file {} from container {}",
                                listItem.getUri(),
                                container.getName(),
                                exc
                            );
                        }
                    });
            });
    }

    private boolean canBeDeleted(CloudBlockBlob blob) throws StorageException {
        blob.downloadAttributes();

        Date createdTime = blob.getProperties().getLastModified();
        Date cutoff = Date.from(Instant.now().minus(this.deleteDelay));

        return createdTime.before(cutoff);
    }
}
