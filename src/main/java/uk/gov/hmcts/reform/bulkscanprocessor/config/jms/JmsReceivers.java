package uk.gov.hmcts.reform.bulkscanprocessor.config.jms;

import jakarta.jms.JMSException;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.jms.JmsProcessedEnvelopeNotificationHandler;

/**
 * JMS receivers configuration.
 */
@Configuration()
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsReceivers {

    private static final Logger log = LoggerFactory.getLogger(JmsReceivers.class);

    private final JmsProcessedEnvelopeNotificationHandler messageHandler;

    /**
     * Constructor.
     * @param messageHandler The message handler
     */
    public JmsReceivers(
        JmsProcessedEnvelopeNotificationHandler messageHandler
    ) {
        this.messageHandler = messageHandler;
    }

    /**
     * Receives processed envelopes.
     * @param message The message
     * @throws JMSException JMSException
     */
    @JmsListener(destination = "processed-envelopes", containerFactory = "processedEnvelopesQueueConnectionFactory")
    public void receiveJmsMessage(ActiveMQMessage message) throws JMSException {
        String messageBody = ((ActiveMQTextMessage) message).getText();
        log.info("Received processed-envelopes message {}. Delivery count is: {}",
                 messageBody, message.getStringProperty("JMSXDeliveryCount")
        );
        messageHandler.processMessage(message, messageBody);
        log.info("Message finished/completed");
    }
}
