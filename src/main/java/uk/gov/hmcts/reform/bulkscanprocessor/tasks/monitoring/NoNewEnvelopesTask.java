package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.alerting.NewEnvelopesChecker;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Component
@ConditionalOnProperty("monitoring.no-new-envelopes.enabled")
public class NoNewEnvelopesTask {

    public static final String JOB_NAME = "no-new-envelopes";
    private static final Logger log = LoggerFactory.getLogger(NoNewEnvelopesTask.class);

    private final NewEnvelopesChecker checker;

    public NoNewEnvelopesTask(NewEnvelopesChecker checker) {
        this.checker = checker;
    }

    @Scheduled(cron = "0 0 * * * *", zone = EUROPE_LONDON)
    @SchedulerLock(name = JOB_NAME, lockAtLeastFor = "30s")
    public void run() {
        log.info("Starting {} job", JOB_NAME);
        checker.checkIfEnvelopesAreMissing();
        log.info("Finished {} job", JOB_NAME);
    }
}
