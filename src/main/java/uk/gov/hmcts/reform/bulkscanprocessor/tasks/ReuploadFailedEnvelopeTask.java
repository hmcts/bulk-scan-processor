package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the blobs from Azure Blob storage and will do below things
 * 1. Reads Blob from container.
 * 2. Extract Zip file(Blob)
 * 3. Transform metadata json to DB entities.
 * 4. Save PDF files in document storage.
 * 5. Update status and doc urls in DB.
 */
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class ReuploadFailedEnvelopeTask {

    private static final Logger log = LoggerFactory.getLogger(ReuploadFailedEnvelopeTask.class);

    private final CloudBlobClient cloudBlobClient;
    private final DocumentProcessor documentProcessor;
    private final EnvelopeProcessor envelopeProcessor;
    private final ErrorHandlingWrapper errorWrapper;

    public ReuploadFailedEnvelopeTask(
        CloudBlobClient cloudBlobClient,
        DocumentProcessor documentProcessor,
        EnvelopeProcessor envelopeProcessor,
        ErrorHandlingWrapper errorWrapper
    ) {
        this.cloudBlobClient = cloudBlobClient;
        this.documentProcessor = documentProcessor;
        this.envelopeProcessor = envelopeProcessor;
        this.errorWrapper = errorWrapper;
    }

    @SchedulerLock(name = "re-upload-failures")
    @Scheduled(fixedDelayString = "${scheduling.task.reupload.delay}")
    public void processUploadFailures() {
        Map<String, List<Envelope>> envelopes = envelopeProcessor.getFailedToUploadEnvelopes()
            .stream()
            .collect(Collectors.groupingBy(Envelope::getContainer));

        envelopes.forEach(this::processEnvelopes);
    }

    private void processEnvelopes(String containerName, List<Envelope> envelopes) {
        log.info("Processing {} failed documents for container {}", envelopes.size(), containerName);
    }
}
