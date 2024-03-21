package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.Duration;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

/**
 * Task to monitor incomplete envelopes.
 */
@Component
@ConditionalOnProperty(prefix = "monitoring.incomplete-envelopes", name = "enabled")
public class IncompleteEnvelopesTask {

    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesTask.class);

    private static final String TASK_NAME = "incomplete-envelopes-monitoring";

    private final EnvelopeRepository envelopeRepository;
    private final Duration staleAfter;

    /**
     * Constructor for the IncompleteEnvelopesTask.
     * @param envelopeRepository The envelope repository
     * @param staleAfter The duration after which an envelope is considered stale
     */
    public IncompleteEnvelopesTask(
        EnvelopeRepository envelopeRepository,
        @Value("${monitoring.incomplete-envelopes.stale-after}") Duration staleAfter
    ) {
        this.envelopeRepository = envelopeRepository;
        this.staleAfter = staleAfter;
    }

    /**
     * Runs the task.
     */
    @Scheduled(cron = "${monitoring.incomplete-envelopes.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = TASK_NAME, lockAtLeastFor = "10s")
    public void run() {
        log.info("Started {} job", TASK_NAME);

        int incompleteEnvelopes = envelopeRepository.getIncompleteEnvelopesCountBefore(now().minus(staleAfter));

        if (incompleteEnvelopes > 0) {
            // warning as to not mix up with existing alerting on all exception
            // which suppose to be unknown or unhandled ones. and it is not really application error
            // **this log line is used in alerting**. be aware before making any changes
            log.warn("There are {} incomplete envelopes as of {}", incompleteEnvelopes, now());
        }

        log.info("Finished {} job", TASK_NAME);
    }
}
