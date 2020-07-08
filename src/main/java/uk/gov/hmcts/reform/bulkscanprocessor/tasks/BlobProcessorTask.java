package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobInputStream;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileLoadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EligibilityChecker;
import uk.gov.hmcts.reform.bulkscanprocessor.services.FileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static org.apache.commons.io.IOUtils.toByteArray;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
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

    private final EnvelopeProcessor envelopeProcessor;

    private final EligibilityChecker eligibilityChecker;

    private final FileContentProcessor fileContentProcessor;

    public BlobProcessorTask(
            BlobManager blobManager,
            EnvelopeProcessor envelopeProcessor,
            EligibilityChecker eligibilityChecker,
            FileContentProcessor fileContentProcessor
    ) {
        this.blobManager = blobManager;
        this.envelopeProcessor = envelopeProcessor;
        this.eligibilityChecker = eligibilityChecker;
        this.fileContentProcessor = fileContentProcessor;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.scan.delay}")
    public void processBlobs() {
        log.info("Started blob processing job");

        for (CloudBlobContainer container : blobManager.listInputContainers()) {
            processZipFiles(container);
        }

        log.info("Finished blob processing job");
    }

    private void processZipFiles(CloudBlobContainer container) {
        log.info("Processing blobs for container {}", container.getName());
        List<String> zipFilenames = getShuffledZipFileNames(container);

        for (String zipFilename : zipFilenames) {
            tryProcessZipFile(container, zipFilename);
        }

        log.info("Finished processing blobs for container {}", container.getName());
    }

    private void tryProcessZipFile(CloudBlobContainer container, String zipFilename) {
        try {
            processZipFileIfEligible(container, zipFilename);
        } catch (Exception ex) {
            log.error("Failed to process file {} from container {}", zipFilename, container.getName(), ex);
        }
    }

    private void processZipFileIfEligible(CloudBlobContainer container, String zipFilename)
        throws IOException, StorageException, URISyntaxException {
        // this log entry is used in alerting. Ticket: BPS-541
        log.info("Processing zip file {} from container {}", zipFilename, container.getName());

        CloudBlockBlob cloudBlockBlob = container.getBlockBlobReference(zipFilename);

        if (eligibilityChecker.isEligibleForProcessing(cloudBlockBlob, container.getName(), zipFilename)) {
            cloudBlockBlob.downloadAttributes();
            leaseAndProcessZipFile(container, cloudBlockBlob, zipFilename);
        }
    }

    private void leaseAndProcessZipFile(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        String zipFilename
    ) throws StorageException, IOException {
        Optional<String> leaseIdOption = blobManager.acquireLease(cloudBlockBlob, container.getName(), zipFilename);

        if (leaseIdOption.isPresent()) {
            String leaseId = leaseIdOption.get();

            try {
                processZipFile(container, cloudBlockBlob, zipFilename, leaseId);
            } finally {
                blobManager.tryReleaseLease(cloudBlockBlob, container.getName(), zipFilename, leaseId);
            }
        }
    }

    private void processZipFile(
        CloudBlobContainer container,
        CloudBlockBlob cloudBlockBlob,
        String zipFilename,
        String leaseId
    ) throws StorageException, IOException {
        Envelope envelope = envelopeProcessor.getEnvelopeByFileAndContainer(container.getName(), zipFilename);

        if (envelope == null) {
            // Zip file will include metadata.json and collection of pdf documents
            try (ZipInputStream zis = loadIntoMemory(cloudBlockBlob, zipFilename)) {
                envelopeProcessor.createEvent(
                    ZIPFILE_PROCESSING_STARTED,
                    container.getName(),
                    zipFilename,
                    null,
                    null
                );

                fileContentProcessor.processZipFileContent(zis, zipFilename, container.getName(), leaseId);
            }
        } else {
            log.info(
                "Envelope already exists for container {} and file {} - aborting its processing. Envelope ID: {}",
                container.getName(),
                zipFilename,
                envelope.getId()
            );
        }
    }

    private ZipInputStream loadIntoMemory(CloudBlockBlob cloudBlockBlob, String zipFilename) throws StorageException {
        log.info("Loading file {} into memory.", zipFilename);
        try (BlobInputStream blobInputStream = cloudBlockBlob.openInputStream()) {
            byte[] array = toByteArray(blobInputStream);
            log.info(
                "Finished loading file {} into memory. {} loaded.",
                zipFilename,
                FileUtils.byteCountToDisplaySize(array.length)
            );
            return new ZipInputStream(new ByteArrayInputStream(array));
        } catch (IOException exception) {
            throw new ZipFileLoadException("Error loading blob file " + zipFilename, exception);
        }
    }
}
