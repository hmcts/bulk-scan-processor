package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

@Lazy
@Configuration
public class ServiceBusConfiguration {

    public static final String ENVELOPE_QUEUE_PUSH = "envelope-queue-push";
    public static final String NOTIFICATION_QUEUE_PUSH = "notification-queue-push";

    @Bean(name = ENVELOPE_QUEUE_PUSH)
    public ServiceBusHelper envelopesQueueHelper(
        @Value("${queues.envelopes.connection-string}") String connectionString,
        @Value("${queues.envelopes.queue-name}") String queueName,
        ObjectMapper objectMapper
    ) throws InterruptedException, ServiceBusException {
        return new ServiceBusHelper(
            new QueueClient(
                new ConnectionStringBuilder(connectionString, queueName),
                ReceiveMode.PEEKLOCK
            ),
            objectMapper
        );
    }

    @Bean(name = NOTIFICATION_QUEUE_PUSH)
    public ServiceBusHelper notificationsQueueHelper(
        @Value("${queues.notifications.connection-string}") String connectionString,
        @Value("${queues.notifications.queue-name}") String queueName,
        ObjectMapper objectMapper
    ) throws InterruptedException, ServiceBusException {
        return new ServiceBusHelper(
            new QueueClient(
                new ConnectionStringBuilder(connectionString, queueName),
                ReceiveMode.PEEKLOCK
            ),
            objectMapper
        );
    }
}
