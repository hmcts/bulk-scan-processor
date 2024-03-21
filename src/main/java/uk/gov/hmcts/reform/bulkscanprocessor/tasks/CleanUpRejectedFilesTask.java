package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.LeaseAcquirer;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.time.Duration;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the rejected files from Azure Blob storage and will delete them if they are older than the configured
 * time to live (ttl).
 */
@Service
@ConditionalOnProperty(value = "scheduling.task.delete-rejected-files.enabled")
public class CleanUpRejectedFilesTask {

    private static final Logger log = LoggerFactory.getLogger(CleanUpRejectedFilesTask.class);
    private static final String TASK_NAME = "delete-rejected-files";

    private final BlobManager blobManager;
    private final LeaseAcquirer leaseAcquirer;
    private final Duration ttl;
    private static final ListBlobsOptions listOptions =
        new ListBlobsOptions().setDetails(new BlobListDetails().setRetrieveSnapshots(true));

    /**
     * Constructor for the CleanUpRejectedFilesTask.
     * @param blobManager The blob manager
     * @param leaseAcquirer The lease acquirer
     * @param ttl The time to live for rejected files
     */
    public CleanUpRejectedFilesTask(
        BlobManager blobManager,
        LeaseAcquirer leaseAcquirer,
        @Value("${scheduling.task.delete-rejected-files.ttl}") String ttl // ISO-8601 duration string
    ) {
        this.blobManager = blobManager;
        this.leaseAcquirer = leaseAcquirer;
        this.ttl = Duration.parse(ttl);
    }

    /**
     * This method is executed by Scheduler as per configured interval.
     * It will read all the rejected files from Azure Blob storage and will delete them if they are older than the configured
     * time to live (ttl).
     */
    @Scheduled(cron = "${scheduling.task.delete-rejected-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        log.info("Started {} job", TASK_NAME);

        blobManager
            .listRejectedContainers()
            .forEach(this::deleteFilesInRejectedContainer);

        log.info("Finished {} job", TASK_NAME);
    }

    /**
     * Deletes files in the rejected container that are older than the configured time to live (ttl).
     * @param containerClient The rejected container client
     */
    private void deleteFilesInRejectedContainer(BlobContainerClient containerClient) {

        var containerName = containerClient.getBlobContainerName();
        log.info("Looking for rejected files to delete. Container: {}", containerName);

        containerClient
            .listBlobs(listOptions, null)
            .stream()
            .filter(this::canBeDeleted)
            .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
            .forEach(blobClient -> leaseAcquirer.ifAcquiredOrElse(
                blobClient,
                leaseId -> deleteBlob(blobClient),
                errorCode -> {}, // nothing to do if blob not found in rejected container
                false
            ));

        log.info("Finished removing rejected files. Container: {}", containerName);
    }

    /**
     * Deletes the blob from the container.
     * @param blobClient The blob client
     */
    private void deleteBlob(BlobClient blobClient) {
        try {
            blobClient.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                null,
                Context.NONE
            );
            log.info(
                "Deleted rejected file {} from container {}",
                blobClient.getBlobName(),
                blobClient.getContainerName()
            );
        } catch (Exception ex) {
            log.error(
                "Unable to delete rejected file {} from container {}",
                blobClient.getBlobName(),
                blobClient.getContainerName(),
                ex
            );
        }
    }

    /**
     * Checks if the blob can be deleted.
     * @param blobItem The blob item
     * @return true if the blob can be deleted, false otherwise
     */
    private boolean canBeDeleted(BlobItem blobItem) {
        // getLastModified method returns time in UTC so use UTC time
        return blobItem
            .getProperties()
            .getLastModified()
            .plus(ttl)
            .isBefore(now(UTC));
    }
}
