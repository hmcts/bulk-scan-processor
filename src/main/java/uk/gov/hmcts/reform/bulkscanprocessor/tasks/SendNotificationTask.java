package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;

/**
 * Sends notifications to the orchestrator containing processed envelopes.
 */
public class SendNotificationTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendNotificationTask.class);

    private final ServiceBusHelper serviceBusHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    // region constructor
    public SendNotificationTask(
        ServiceBusHelper serviceBusHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.serviceBusHelper = serviceBusHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }
    // endregion

    public void run() {
        envelopeRepo
            .findByStatus(PROCESSED)
            .forEach(env -> {
                try {
                    serviceBusHelper.sendMessage(new EnvelopeMsg(env));
                    createEvent(env);
                    updateStatus(env);
                } catch (Exception exc) {
                    // log error and try with another envelope.
                    LOGGER.error("Error sending envelope notification", exc);
                }
            });
    }

    private void createEvent(Envelope envelope) {
        processEventRepo
            .save(
                new ProcessEvent(
                    envelope.getContainer(),
                    envelope.getZipFileName(),
                    Event.DOC_PROCESSED_NOTIFICATION_SENT
                )
            );
    }

    private void updateStatus(Envelope envelope) {
        envelope.setStatus(Status.NOTIFICATION_SENT);
        envelopeRepo.save(envelope);
    }
}
