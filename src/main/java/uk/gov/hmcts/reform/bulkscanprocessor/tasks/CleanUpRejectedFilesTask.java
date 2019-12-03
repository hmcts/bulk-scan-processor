package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;

import static com.microsoft.azure.storage.blob.BlobListingDetails.SNAPSHOTS;
import static com.microsoft.azure.storage.blob.DeleteSnapshotsOption.INCLUDE_SNAPSHOTS;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Service
@ConditionalOnProperty(value = "scheduling.task.delete-rejected-files.enabled")
public class CleanUpRejectedFilesTask {

    private static final Logger log = LoggerFactory.getLogger(CleanUpRejectedFilesTask.class);

    private final BlobManager blobManager;
    private final Duration ttl;

    // region constructor
    public CleanUpRejectedFilesTask(
        BlobManager blobManager,
        @Value("${scheduling.task.delete-rejected-files.ttl}") String ttl // ISO-8601 duration string
    ) {
        this.blobManager = blobManager;
        this.ttl = Duration.parse(ttl);
    }
    // endregion

    @Scheduled(cron = "${scheduling.task.delete-rejected-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "delete-rejected-files")
    public void run() {
        log.info("Scanning for old rejected files");
        blobManager
            .listRejectedContainers()
            .forEach(container -> {
                log.info("Scanning for old rejected files in container {}", container.getName());
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
        Date cutoff = Date.from(Instant.now().minus(this.ttl));

        return createdTime.before(cutoff);
    }
}
