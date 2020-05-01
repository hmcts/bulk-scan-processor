package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.UploadEnvelopeDocumentsService;

import static java.util.stream.Collectors.groupingBy;

@Component
@ConditionalOnProperty(value = "scheduling.task.reupload.enabled", matchIfMissing = true)
public class ReuploadFailedEnvelopeTask {

    private static final Logger log = LoggerFactory.getLogger(ReuploadFailedEnvelopeTask.class);

    private final EnvelopeRepository envelopeRepository;
    private final UploadEnvelopeDocumentsService uploadService;
    private final int maxReUploadTriesCount;

    public ReuploadFailedEnvelopeTask(
        EnvelopeRepository envelopeRepository,
        UploadEnvelopeDocumentsService uploadService,
        @Value("${scheduling.task.reupload.max_tries}") int maxReUploadTriesCount
    ) {
        this.envelopeRepository = envelopeRepository;
        this.uploadService = uploadService;
        this.maxReUploadTriesCount = maxReUploadTriesCount;
    }

    @SchedulerLock(name = "re-upload-failures")
    @Scheduled(fixedDelayString = "${scheduling.task.reupload.delay}")
    public void processUploadFailures() {
        log.info("Started failed document processing job");

        envelopeRepository
            .findEnvelopesToResend(maxReUploadTriesCount)
            .stream()
            .collect(groupingBy(Envelope::getContainer))
            .forEach(uploadService::processByContainer);

        log.info("Finished failed document processing job");
    }
}
