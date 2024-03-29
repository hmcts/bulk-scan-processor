package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultRedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import java.util.UUID;
import javax.jms.ConnectionFactory;

public class JmsProcessedEnvelopeMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(JmsProcessedEnvelopeMessageSender.class);

    private static final JmsTemplate JMS_TEMPLATE = new JmsTemplate();

    public JmsProcessedEnvelopeMessageSender() {
        // Set the connection factory only once in the constructor
        JMS_TEMPLATE.setConnectionFactory(getTestFactory());
        JMS_TEMPLATE.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds
    }

    /**
     * Sends a message to processed-envelopes JMS queue.
     */
    public void sendProcessedEnvelopeMessage(UUID envelopeId, String ccdId, String ccdAction) {
        String message = " {"
            + "\"envelope_id\":\"" + envelopeId + "\","
            + "\"ccd_id\":\"" + ccdId + "\","
            + "\"envelope_ccd_action\":\"" + ccdAction + "\","
            + "\"dummy\":\"value-should-ignore\""
            + "}";

        JMS_TEMPLATE.convertAndSend("processed-envelopes", message);

        logger.info("Sent message to processed-envelopes JMS queue for envelope id {}", envelopeId);
    }

    public ConnectionFactory getTestFactory() {
        String connection = String.format("amqp://localhost:%1s?amqp.idleTimeout=%2d", "5672", 30000);
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connection);
        jmsConnectionFactory.setUsername("admin");
        jmsConnectionFactory.setPassword("admin");
        JmsDefaultRedeliveryPolicy jmsDefaultRedeliveryPolicy = new JmsDefaultRedeliveryPolicy();
        jmsDefaultRedeliveryPolicy.setMaxRedeliveries(3);
        jmsConnectionFactory.setRedeliveryPolicy(jmsDefaultRedeliveryPolicy);
        jmsConnectionFactory.setClientID(UUID.randomUUID().toString());
        return new CachingConnectionFactory(jmsConnectionFactory);
    }
}
