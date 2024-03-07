package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.HeartbeatMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;

/**
 * Sends heartbeat messages to orchestrator.
 */
@ConditionalOnProperty(value = "scheduling.task.notifications_to_orchestrator.enabled", matchIfMissing = true)
@Component
@ConditionalOnExpression("!${jms.enabled}")
public class HeartbeatTask {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatTask.class);

    private final ServiceBusSendHelper serviceBusHelper;

    public HeartbeatTask(@Qualifier("envelopes-helper") ServiceBusSendHelper serviceBusHelper) {
        this.serviceBusHelper = serviceBusHelper;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @SchedulerLock(name = "heartbeat")
    public void run() {
        try {
            serviceBusHelper.sendMessage(new HeartbeatMsg());
            log.debug("Heartbeat sent");
        } catch (Exception exc) {
            log.error("Error sending heartbeat message", exc);
        }
    }
}
