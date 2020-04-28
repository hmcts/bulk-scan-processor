package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.groupingBy;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;

@Component
public class UploadEnvelopeDocumentsTask {

    private static final Logger log = getLogger(UploadEnvelopeDocumentsTask.class);
    private static final String TASK_NAME = "upload-documents";

    private final long minimumEnvelopeAge;
    private final EnvelopeRepository envelopeRepository;
    private final UploadEnvelopeDocumentsService uploadService;

    public UploadEnvelopeDocumentsTask(
        // only process envelopes after defined minutes has passed
        // can be removed once uploading feature is removed from main job
        @Value("${scheduling.task." + TASK_NAME + ".envelope-age-in-minutes}") long minimumEnvelopeAge,
        EnvelopeRepository envelopeRepository,
        UploadEnvelopeDocumentsService uploadService
    ) {
        this.minimumEnvelopeAge = minimumEnvelopeAge;
        this.envelopeRepository = envelopeRepository;
        this.uploadService = uploadService;
    }

    @SchedulerLock(name = TASK_NAME) // so to not upload documents multiple times
    public void run() {
        log.info("Started {} job", TASK_NAME);

        envelopeRepository
            .findByStatus(CREATED)
            .stream()
            // will be removed after upload is removed from scanning task
            .filter(this::isEnvelopeReady)
            .collect(groupingBy(Envelope::getContainer))
            .forEach(uploadService::processByContainer);

        log.info("Finished {} job", TASK_NAME);
    }

    private boolean isEnvelopeReady(Envelope envelope) {
        return envelope.getCreatedAt().isBefore(now().minus(minimumEnvelopeAge, MINUTES));
    }
}
