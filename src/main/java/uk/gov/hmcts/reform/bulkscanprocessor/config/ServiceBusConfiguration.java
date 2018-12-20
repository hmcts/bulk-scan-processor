package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Lazy
@Configuration
public class ServiceBusConfiguration {

    static final ExecutorService QUEUE_READ_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "notifications-queue-read")
    );

    @Bean(name = "envelopes")
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

    @Bean(name = "notifications")
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

    @Bean(name = "notifications-read")
    @ConditionalOnProperty(prefix = "queues.read-notifications", name = "enabled")
    public IQueueClient notificationsQueueReader(
        @Value("${queues.read-notifications.connection-string}") String connectionString,
        @Value("${queues.read-notifications.queue-name}") String queueName,
        ErrorNotificationService service,
        ObjectMapper objectMapper
    ) throws InterruptedException, ServiceBusException {
        IQueueClient client = new QueueClient(
            new ConnectionStringBuilder(connectionString, queueName),
            ReceiveMode.PEEKLOCK
        );

        IMessageHandler messageHandler = new ErrorNotificationHandler(service, objectMapper);

        client.registerMessageHandler(messageHandler, QUEUE_READ_EXEC);

        return client;
    }
}
