package uk.gov.hmcts.reform.bulkscanprocessor.services;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to handle sending notifications to orchestrator.
 */
@Service
@ConditionalOnExpression("!${jms.enabled}")
public class OrchestratorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(OrchestratorNotificationService.class);

    private final ServiceBusSendHelper serviceBusHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    /**
     * Constructor for the OrchestratorNotificationService.
     * @param serviceBusHelper The service bus helper
     * @param envelopeRepo The repository for envelope
     * @param processEventRepo The repository for process event
     */
    public OrchestratorNotificationService(
        @Qualifier("envelopes-helper") ServiceBusSendHelper serviceBusHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.serviceBusHelper = serviceBusHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }

    /**
     * Process envelope.
     * @param successCount The success count
     * @param env The envelope
     */
    @Transactional
    public void processEnvelope(AtomicInteger successCount, Envelope env) {
        updateStatus(env);
        createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_SENT);
        serviceBusHelper.sendMessage(new EnvelopeMsg(env));
        logEnvelopeSent(env);
        successCount.incrementAndGet();
    }

    /**
     * Process envelope.
     * @param env The envelope
     */
    private void logEnvelopeSent(Envelope env) {
        log.info(
            "Sent envelope with ID {}. File {}, container {}",
            env.getId(),
            env.getZipFileName(),
            env.getContainer()
        );
    }

    /**
     * Create event.
     * @param envelope The envelope
     * @param event The event
     */
    private void createEvent(Envelope envelope, Event event) {
        processEventRepo.saveAndFlush(
            new ProcessEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                event
            )
        );
    }

    /**
     * Update status.
     * @param envelope The envelope
     */
    private void updateStatus(Envelope envelope) {
        envelope.setStatus(Status.NOTIFICATION_SENT);
        envelopeRepo.saveAndFlush(envelope);

        log.info("Envelope {} status changed to NOTIFICATION_SENT. Case reference: {}", envelope.getZipFileName(),
                 envelope.getCaseNumber());
    }
}
