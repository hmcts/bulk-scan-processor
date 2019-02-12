package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

@Configuration
@AutoConfigureAfter(QueueClientConfig.class)
@Profile("!nosb") // no servicebus queue handler registration
public class ServiceBusConfiguration {

    @Autowired
    private ObjectMapper objectMapper;


    @Bean(name = "envelopes-helper")
    public ServiceBusHelper envelopesQueueHelper(
        @Qualifier("envelopes-client") IQueueClient queueClient
    ) {
        return new ServiceBusHelper(queueClient, objectMapper);
    }

    @Bean(name = "notifications-helper")
    public ServiceBusHelper notificationsQueueHelper(
        @Qualifier("notifications-client") IQueueClient queueClient
    ) {
        return new ServiceBusHelper(queueClient, objectMapper);
    }

    @Bean(name = "read-notifications-completor")
    @ConditionalOnProperty(name = "queues.read-notifications.enabled", havingValue = "true")
    public MessageAutoCompletor readNotificationsMessageCompletor(
        @Qualifier("read-notifications-client") IQueueClient queueClient
    ) {
        return new MessageAutoCompletor(queueClient);
    }
}
