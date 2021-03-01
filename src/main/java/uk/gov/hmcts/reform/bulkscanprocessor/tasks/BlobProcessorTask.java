package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ZipFileProcessingService;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.services.FileNamesExtractor.getShuffledZipFileNames;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things:
 * <ol>
 * <li>Read Blob from container by acquiring lease</li>
 * <li>Extract Zip file (blob)</li>
 * <li>Transform metadata json to DB entities</li>
 * <li>Save PDF files in document storage</li>
 * <li>Update status and doc urls in DB</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.scan.enabled", matchIfMissing = true)
public class BlobProcessorTask {

    private static final Logger log = LoggerFactory.getLogger(BlobProcessorTask.class);

    private final BlobManager blobManager;

    private final ZipFileProcessingService zipFileProcessingService;

    public BlobProcessorTask(
        BlobManager blobManager,
        ZipFileProcessingService zipFileProcessingService
    ) {
        this.blobManager = blobManager;
        this.zipFileProcessingService = zipFileProcessingService;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() {
        log.info("Started blob processing job");

        for (BlobContainerClient container : blobManager.listInputContainerClients()) {
            processZipFiles(container);
        }

        log.info("Finished blob processing job");
    }

    private void processZipFiles(BlobContainerClient container) {
        log.info("Processing blobs for container {}", container.getBlobContainerName());
        List<String> zipFilenames = getShuffledZipFileNames(container);

        for (String zipFilename : zipFilenames) {
            zipFileProcessingService.tryProcessZipFile(container, zipFilename);
        }

        log.info("Finished processing blobs for container {}", container.getBlobContainerName());
    }
}
