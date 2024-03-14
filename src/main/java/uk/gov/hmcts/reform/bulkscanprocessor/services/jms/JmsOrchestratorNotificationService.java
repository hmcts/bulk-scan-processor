package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

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

import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnExpression("${jms.enabled}")
public class JmsOrchestratorNotificationService {
    private static final Logger log = LoggerFactory.getLogger(JmsOrchestratorNotificationService.class);

    private final JmsQueueSendHelper jmsQueueSendHelper;
    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository processEventRepo;

    public JmsOrchestratorNotificationService(
        @Qualifier("jms-envelopes-helper") JmsQueueSendHelper jmsQueueSendHelper,
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository processEventRepo
    ) {
        this.jmsQueueSendHelper = jmsQueueSendHelper;
        this.envelopeRepo = envelopeRepo;
        this.processEventRepo = processEventRepo;
    }

    @Transactional
    public void processEnvelope(AtomicInteger successCount, Envelope env) {
        updateStatus(env);
        createEvent(env, Event.DOC_PROCESSED_NOTIFICATION_SENT);
        jmsQueueSendHelper.sendMessage(new EnvelopeMsg(env));
        logEnvelopeSent(env);
        successCount.incrementAndGet();
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

        log.info("Envelope {} status changed to NOTIFICATION_SENT", envelope.getZipFileName());
    }
}
