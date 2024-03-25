package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.services.DeleteFilesService;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the complete files from Azure Blob storage and will delete them.
 */
@Service
@ConditionalOnProperty(value = "scheduling.task.delete-complete-files.enabled")
public class DeleteCompleteFilesTask {
    private static final Logger log = LoggerFactory.getLogger(DeleteCompleteFilesTask.class);

    private static final String TASK_NAME = "delete-complete-files";

    private final BlobManager blobManager;
    private final DeleteFilesService deleteFilesService;

    /**
     * Constructor for the DeleteCompleteFilesTask.
     * @param blobManager The blob manager
     * @param deleteFilesService The delete files service
     */
    public DeleteCompleteFilesTask(
        BlobManager blobManager,
        DeleteFilesService deleteFilesService
    ) {
        this.blobManager = blobManager;
        this.deleteFilesService = deleteFilesService;
    }

    /**
     * This method is executed by Scheduler as per configured interval.
     * It will read all the complete files from Azure Blob storage and will delete them.
     */
    @Scheduled(cron = "${scheduling.task.delete-complete-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME)
    public void run() {
        log.info("Started {} job", TASK_NAME);

        for (BlobContainerClient container : blobManager.listInputContainerClients()) {
            try {
                deleteFilesService.processCompleteFiles(container);
            } catch (Exception ex) {
                log.error(
                    "Failed to process files from container {}",
                    container.getBlobContainerName(),
                    ex
                );
            }
        }

        log.info("Finished {} job", TASK_NAME);
    }
}
