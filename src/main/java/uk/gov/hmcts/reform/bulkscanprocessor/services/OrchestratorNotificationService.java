package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.Transactional;

@Service
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
public class OrchestratorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorNotificationService.class);

    private final ServiceBusHelper serviceBusHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    public OrchestratorNotificationService(
        @Qualifier("envelopes-helper") ServiceBusHelper serviceBusHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.serviceBusHelper = serviceBusHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }

    @Transactional
    public void processEnvelope(AtomicInteger successCount, Envelope env) {
        try {
            updateStatus(env);
            serviceBusHelper.sendMessage(new EnvelopeMsg(env));
            logEnvelopeSent(env);
            createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_SENT);
            successCount.incrementAndGet();
        } catch (Exception exc) {
            createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_FAILURE);
            // log error and try with another envelope.
            log.error("Error sending envelope notification", exc);
        }
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
        processEventRepo.saveAndFlush(
            new ProcessEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                event
            )
        );
    }

    private void updateStatus(Envelope envelope) {
        envelope.setStatus(Status.NOTIFICATION_SENT);
        envelopeRepo.saveAndFlush(envelope);
    }
}
