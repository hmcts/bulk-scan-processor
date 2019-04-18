package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "monitoring.incomplete-envelopes", name = "enabled")
public class IncompleteEnvelopesTask {

    private static final Logger log = LoggerFactory.getLogger(IncompleteEnvelopesTask.class);

    @SuppressWarnings("all") // tmp until implemented
    public IncompleteEnvelopesTask(
        // autowire repository
    ) {
        // empty constructor
    }

    @Scheduled(cron = "${monitoring.incomplete-envelopes.cron}")
    @SchedulerLock(name = "incomplete-envelopes-monitoring")
    public void run() {
        log.info("Checking for incomplete envelopes");

        // get data and log here

        log.debug("Finished checking for incomplete envelopes");
    }
}
