package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

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

import static uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceBusConfiguration.ENVELOPE_QUEUE_PUSH;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
@Component
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
public class OrchestratorNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorNotificationTask.class);

    private final ServiceBusHelper serviceBusHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    // region constructor
    public OrchestratorNotificationTask(
        @Qualifier(ENVELOPE_QUEUE_PUSH) ServiceBusHelper serviceBusHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.serviceBusHelper = serviceBusHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }
    // endregion

    @Scheduled(fixedDelayString = "${scheduling.task.notifications_to_orchestrator.delay}")
    public void run() {
        envelopeRepo
            .findByStatus(PROCESSED)
            .forEach(env -> {
                try {
                    serviceBusHelper.sendMessage(new EnvelopeMsg(env));
                    createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_SENT);
                    updateStatus(env);
                } catch (Exception exc) {
                    createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_FAILURE);
                    // log error and try with another envelope.
                    LOGGER.error("Error sending envelope notification", exc);
                }
            });
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
