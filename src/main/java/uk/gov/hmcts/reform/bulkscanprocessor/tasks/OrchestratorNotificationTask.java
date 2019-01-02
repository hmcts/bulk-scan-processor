package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.List;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
public class OrchestratorNotificationTask {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorNotificationTask.class);

    private final ServiceBusHelper serviceBusHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    // region constructor
    public OrchestratorNotificationTask(
        @Qualifier("envelopes") ServiceBusHelper serviceBusHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.serviceBusHelper = serviceBusHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }
    // endregion

    @SchedulerLock(name = "send-orchestrator-notification")
    @Scheduled(fixedDelayString = "${scheduling.task.notifications_to_orchestrator.delay}")
    public void run() {
        log.info("Started sending notifications to orchestrator");

        List<Envelope> envelopesToSend = envelopeRepo.findByStatus(PROCESSED);

        int successCount = (int)envelopesToSend
            .stream()
            .filter(env -> {
                try {
                    serviceBusHelper.sendMessage(new EnvelopeMsg(env));
                    logEnvelopeSent(env);
                    createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_SENT);
                    updateStatus(env);
                    return true;
                } catch (Exception exc) {
                    createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_FAILURE);
                    // log error and try with another envelope.
                    log.error("Error sending envelope notification", exc);
                    return false;
                }
            })
            .count();

        log.info(
            "Finished sending notifications to orchestrator. Successful: {}. Failures {}.",
            successCount,
            envelopesToSend.size() - successCount
        );
    }

    private void logEnvelopeSent(Envelope env) {
        log.info(
            "Sent envelope with ID {}. File {}, container {}",
            env.getId(),
            env.getZipFileName(),
            env.getContainer()
        );
    }

    private void createEvent(Envelope envelope, Event event) {
        processEventRepo.save(
            new ProcessEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                event
            )
        );
    }

    private void updateStatus(Envelope envelope) {
        envelope.setStatus(Status.NOTIFICATION_SENT);
        envelopeRepo.save(envelope);
    }
}
