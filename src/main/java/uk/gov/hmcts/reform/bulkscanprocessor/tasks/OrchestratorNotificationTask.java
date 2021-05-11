package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.OrchestratorNotificationService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
public class OrchestratorNotificationTask {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorNotificationTask.class);
    private static final String TASK_NAME = "send-orchestrator-notification";

    private final OrchestratorNotificationService orchestratorNotificationService;
    private final EnvelopeRepository envelopeRepo;

    // region constructor
    public OrchestratorNotificationTask(
        OrchestratorNotificationService orchestratorNotificationService,
        EnvelopeRepository envelopeRepo
    ) {
        this.orchestratorNotificationService = orchestratorNotificationService;
        this.envelopeRepo = envelopeRepo;
    }
    // endregion

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(fixedDelayString = "${scheduling.task.notifications_to_orchestrator.delay}")
    public void run() {
        log.info("Started {} job", TASK_NAME);

        AtomicInteger successCount = new AtomicInteger(0);

        List<Envelope> envelopesToSend = envelopeRepo.findByStatus(UPLOADED);

        envelopesToSend
            .forEach(env -> {
                orchestratorNotificationService.processEnvelope(successCount, env);
            });

        log.info(
            "Finished sending notifications to orchestrator. Successful: {}. Failures {}.",
            successCount.get(),
            envelopesToSend.size() - successCount.get()
        );
        log.info("Finished {} job", TASK_NAME);
    }
}
