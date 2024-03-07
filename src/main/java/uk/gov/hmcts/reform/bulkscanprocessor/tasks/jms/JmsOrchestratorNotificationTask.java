package uk.gov.hmcts.reform.bulkscanprocessor.tasks.jms;

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
import uk.gov.hmcts.reform.bulkscanprocessor.services.jms.JmsOrchestratorNotificationService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
@ConditionalOnExpression("${jms.enabled}")
public class JmsOrchestratorNotificationTask {

    private static final Logger log = LoggerFactory.getLogger(JmsOrchestratorNotificationTask.class);
    private static final String TASK_NAME = "jms-send-orchestrator-notification";

    private final JmsOrchestratorNotificationService jmsOrchestratorNotificationService;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    // region constructor
    public JmsOrchestratorNotificationTask(
        JmsOrchestratorNotificationService jmsOrchestratorNotificationService,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo) {
        this.jmsOrchestratorNotificationService = jmsOrchestratorNotificationService;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
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
                try {
                    jmsOrchestratorNotificationService.processEnvelope(successCount, env);
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
        log.info("Finished {} job", TASK_NAME);
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
