package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.FailedDocUploadProcessor;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private final List<Mapping> accessMapping;

    public ReuploadFailedEnvelopeTask(EnvelopeAccessProperties accessProperties) {
        this.accessMapping = accessProperties.getMappings();
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
    public void processUploadFailures() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(
            accessMapping.size(),
            r -> new Thread(r, "BSP-REUPLOAD-%d")
        );
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);

        accessMapping
            .parallelStream()
            .map(Mapping::getJurisdiction)
            .forEach(jurisdiction -> {
                FailedDocUploadProcessor processor = getProcessor();

                completionService.submit(() -> {
                    log.info("Processing failed documents for jurisdiction {}", jurisdiction);

                    processor.processJurisdiction(jurisdiction);

                    return null;
                });
            });

        awaitCompletion(completionService);
        executorService.shutdown();
    }

    private void awaitCompletion(CompletionService<Void> completionService) throws InterruptedException {
        int completed = 0;

        while (completed < accessMapping.size()) {
            Future<Void> future = completionService.take(); // blocks if none available

            try {
                future.get(); // waits if necessary
            } catch (InterruptedException | ExecutionException exception) {
                log.error(exception.getMessage(), exception);

                future.cancel(true);

                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                completed++;
            }
        }
    }
}
