package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import net.javacrumbs.shedlock.core.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.HeartbeatMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

/**
 * Sends heartbeat messages to orchestrator
 */
@Component
public class HeartbeatTask {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatTask.class);

    private final ServiceBusHelper serviceBusHelper;

    public HeartbeatTask(@Qualifier("envelopes-helper") ServiceBusHelper serviceBusHelper) {
        this.serviceBusHelper = serviceBusHelper;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    @SchedulerLock(name = "heartbeat")
    public void run() {
        try {
            serviceBusHelper.sendMessage(new HeartbeatMsg());
            log.info("Heartbeat sent");
        } catch (Exception exc) {
            log.error("Error sending heartbeat message", exc);
        }
    }
}
