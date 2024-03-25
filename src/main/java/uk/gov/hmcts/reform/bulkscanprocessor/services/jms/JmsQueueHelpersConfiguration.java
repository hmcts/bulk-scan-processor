package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.config.QueueClientConfig;

/**
 * Configuration for JMS queue helpers.
 */
@AutoConfigureAfter(QueueClientConfig.class)
@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
@ConditionalOnExpression("${jms.enabled}")
public class JmsQueueHelpersConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates a JmsQueueSendHelper for envelopes queue.
     */
    @Bean(name = "jms-envelopes-helper")
    public JmsQueueSendHelper envelopesQueueHelper(
        @Qualifier("envelopes-jms-template") JmsTemplate jmsTemplate
    ) {
        return new JmsQueueSendHelper(jmsTemplate, objectMapper);
    }

    /**
     * Creates a JmsQueueSendHelper for notifications queue.
     */
    @Bean(name = "jms-notifications-helper")
    public JmsQueueSendHelper notificationsQueueHelper(
        @Qualifier("notifications-jms-template") JmsTemplate jmsTemplate
    ) {
        return new JmsQueueSendHelper(jmsTemplate, objectMapper);
    }

}
