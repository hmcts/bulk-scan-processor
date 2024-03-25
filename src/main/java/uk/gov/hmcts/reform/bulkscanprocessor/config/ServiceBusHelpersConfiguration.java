package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusSendHelper;

/**
 * Configuration for Service Bus queue clients.
 */
@AutoConfigureAfter(QueueClientConfig.class)
@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
@ConditionalOnExpression("!${jms.enabled}")
public class ServiceBusHelpersConfiguration {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Bean for envelopes queue helper.
     * @param queueClient The envelopes queue client
     * @return The service bus send helper
     */
    @Bean(name = "envelopes-helper")
    public ServiceBusSendHelper envelopesQueueHelper(
        @Qualifier("envelopes-send-client") ServiceBusSenderClient queueClient
    ) {
        return new ServiceBusSendHelper(queueClient, objectMapper);
    }

    /**
     * Bean for notifications queue helper.
     * @param queueClient The notifications queue client
     * @return The service bus send helper
     */
    @Bean(name = "notifications-helper")
    public ServiceBusSendHelper notificationsQueueHelper(
        @Qualifier("notifications-send-client") ServiceBusSenderClient queueClient
    ) {
        return new ServiceBusSendHelper(queueClient, objectMapper);
    }

}
