package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.LocalDate;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty(prefix = "monitoring.incomplete-envelopes", name = "enabled")
public class IncompleteEnvelopesTask {

    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesTask.class);

    private final EnvelopeRepository envelopeRepository;

    public IncompleteEnvelopesTask(
        EnvelopeRepository envelopeRepository
    ) {
        this.envelopeRepository = envelopeRepository;
    }

    @Scheduled(cron = "${monitoring.incomplete-envelopes.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "incomplete-envelopes-monitoring", lockAtLeastFor = 10_000)
    public void run() {
        log.info("Checking for incomplete envelopes");

        LocalDate now = LocalDate.now();
        int incompleteEnvelopes = envelopeRepository.getIncompleteEnvelopesCountBefore(now);

        if (incompleteEnvelopes > 0) {
            // warning as to not mix up with existing alerting on all exception
            // which suppose to be unknown or unhandled ones. and it is not really application error
            // **this log line is used in alerting**. be aware before making any changes
            log.warn("There are {} incomplete envelopes as of {}", incompleteEnvelopes, now);
        }

        log.debug("Finished checking for incomplete envelopes");
    }
}
