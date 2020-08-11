package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.BlobRequestConditions;
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
import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

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

    // region constructor
    public CleanUpRejectedFilesTask(
        BlobManager blobManager,
        LeaseAcquirer leaseAcquirer,
        @Value("${scheduling.task.delete-rejected-files.ttl}") String ttl // ISO-8601 duration string
    ) {
        this.blobManager = blobManager;
        this.leaseAcquirer = leaseAcquirer;
        this.ttl = Duration.parse(ttl);
    }
    // endregion

    @Scheduled(cron = "${scheduling.task.delete-rejected-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        log.info("Started {} job", TASK_NAME);

        blobManager
            .getRejectedContainers()
            .forEach(this::cleanUpContainer);

        log.info("Finished {} job", TASK_NAME);
    }

    private void cleanUpContainer(BlobContainerClient containerClient) {

        var containerName = containerClient.getBlobContainerName();
        log.info("Looking for rejected files to delete. Container: {}", containerName);

        containerClient
            .listBlobs(listOptions, null)
            .stream()
            .filter(this::canBeDeleted)
            .map(blobItem -> containerClient.getBlobClient(blobItem.getName()))
            .forEach(blobClient -> leaseAcquirer.ifAcquiredOrElse(
                blobClient,
                leaseId -> delete(blobClient, leaseId),
                errorCode -> {}, // nothing to do if blob not found in rejected container
                false
            ));

        log.info("Finished removing rejected files. Container: {}", containerName);
    }

    private void delete(BlobClient blobClient, String leaseId) {
        try {
            blobClient.deleteWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                new BlobRequestConditions().setLeaseId(leaseId),
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

    //ToDo remove log
    private boolean canBeDeleted(BlobItem blobItem) {
        OffsetDateTime lastModified = blobItem.getProperties().getLastModified();
        OffsetDateTime now = now(UTC);
        OffsetDateTime lastTimeToStay = lastModified.plus(ttl);
        log.info(
            "Blob's last modified: {}, now time: {}, lastTimeToStay: {}",
            lastModified,
            now,
            lastTimeToStay
        );
        return lastTimeToStay
            .isBefore(now(UTC));
    }
}
