package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;

import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will read all the envelopes that are ready to be uploaded and will upload them.
 */
@Component
@ConditionalOnProperty(
    name = "scheduling.task." + UploadEnvelopeDocumentsTask.TASK_NAME + ".enabled",
    matchIfMissing = true
)
public class UploadEnvelopeDocumentsTask {

    private static final Logger log = getLogger(UploadEnvelopeDocumentsTask.class);
    static final String TASK_NAME = "upload-documents";

    private final EnvelopeRepository envelopeRepository;
    private final UploadEnvelopeDocumentsService uploadService;
    private final int maxRetries;

    /**
     * Constructor for the UploadEnvelopeDocumentsTask.
     * @param envelopeRepository The envelope repository
     * @param uploadService The upload service
     * @param maxRetries The maximum number of retries
     */
    public UploadEnvelopeDocumentsTask(
        EnvelopeRepository envelopeRepository,
        UploadEnvelopeDocumentsService uploadService,
        @Value("${scheduling.task.upload-documents.max_tries}") int maxRetries
    ) {
        this.envelopeRepository = envelopeRepository;
        this.uploadService = uploadService;
        this.maxRetries = maxRetries;
    }

    /**
     * This method is executed by Scheduler as per configured interval.
     * It will read all the envelopes that are ready to be uploaded and will upload them.
     */
    @Scheduled(fixedDelayString = "${scheduling.task." + TASK_NAME + ".delay}")
    @SchedulerLock(name = TASK_NAME) // so to not upload documents multiple times
    public void run() {
        log.info("Started {} job", TASK_NAME);

        envelopeRepository
            .findEnvelopesToUpload(maxRetries)
            .stream()
            .collect(groupingBy(Envelope::getContainer))
            .forEach(uploadService::processByContainer);

        log.info("Finished {} job", TASK_NAME);
    }
}
