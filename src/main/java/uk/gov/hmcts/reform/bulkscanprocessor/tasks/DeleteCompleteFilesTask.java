package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

@Service
@ConditionalOnProperty(value = "scheduling.task.delete-complete-files.enabled")
public class DeleteCompleteFilesTask {

    private static final Logger log = LoggerFactory.getLogger(DeleteCompleteFilesTask.class);
    private static final String TASK_NAME = "delete-complete-files";

    // region constructor
    public DeleteCompleteFilesTask(
    ) {
        //
    }
    // endregion

    @Scheduled(cron = "${scheduling.task.delete-complete-files.cron}", zone = EUROPE_LONDON)
    @SchedulerLock(name = "delete-complete-files")
    public void run() {
        log.info("Started {} task", TASK_NAME);

        log.info("Finished {} task", TASK_NAME);
    }
}
