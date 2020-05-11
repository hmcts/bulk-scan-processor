package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.FailedDocUploadProcessor;

/**
 * This class is a task executed by Scheduler as per configured interval.
 * It will retrieve failed to upload envelopes from DB and try to re-upload.
 * <ol>
 *     <li>Get batch of failed to upload envelopes</li>
 *     <li>Retrieve relevant zip file from Blob Storage</li>
 *     <li>Try to upload pdfs</li>
 * </ol>
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.reupload.enabled", matchIfMissing = true)
@EnableConfigurationProperties(EnvelopeAccessProperties.class)
public class ReuploadFailedEnvelopeTask {

    private static final Logger log = LoggerFactory.getLogger(ReuploadFailedEnvelopeTask.class);

    public ReuploadFailedEnvelopeTask() {
        // empty constructor
    }

    /**
     * Spring overrides the {@code @Lookup} method and returns an instance of bean.
     *
     * @return Instance of {@code FailedDocUploadProcessor}
     */
    @Lookup
    public FailedDocUploadProcessor getProcessor() {
        return null;
    }

    @SchedulerLock(name = "re-upload-failures")
    @Scheduled(fixedDelayString = "${scheduling.task.reupload.delay}")
    public void processUploadFailures() {
        log.info("Started failed document processing job");

        try {
            getProcessor().processJurisdiction();

            log.info("Finished failed document processing job");
        } catch (Exception ex) {
            log.error("An error occurred while processing failed documents", ex);
        }
    }
}
