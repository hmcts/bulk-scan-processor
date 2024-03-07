package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.OrchestratorNotificationService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
@ConditionalOnExpression("!${jms.enabled}")
public class OrchestratorNotificationTask {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorNotificationTask.class);
    private static final String TASK_NAME = "send-orchestrator-notification";

    private final OrchestratorNotificationService orchestratorNotificationService;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    // region constructor
    public OrchestratorNotificationTask(
        OrchestratorNotificationService orchestratorNotificationService,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo) {
        this.orchestratorNotificationService = orchestratorNotificationService;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }
    // endregion

    @SchedulerLock(name = TASK_NAME)
    @Scheduled(fixedDelayString = "${scheduling.task.notifications_to_orchestrator.delay}")
    public void run() {
        log.debug("Started {} job", TASK_NAME);

        AtomicInteger successCount = new AtomicInteger(0);

        List<Envelope> envelopesToSend = envelopeRepo.findByStatus(UPLOADED);

        envelopesToSend
            .forEach(env -> {
                try {
                    orchestratorNotificationService.processEnvelope(successCount, env);
                } catch (Exception exc) {
                    createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_FAILURE);
                    // log error and try with another envelope.
                    log.error("Error sending envelope notification", exc);
                }
            });

        log.info(
            "Finished sending notifications to orchestrator. Successful: {}. Failures {}.",
            successCount.get(),
            envelopesToSend.size() - successCount.get()
        );
        log.debug("Finished {} job", TASK_NAME);
    }

    private void createEvent(Envelope envelope, Event event) {
        processEventRepo.saveAndFlush(
            new ProcessEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                event
            )
        );
    }
}
