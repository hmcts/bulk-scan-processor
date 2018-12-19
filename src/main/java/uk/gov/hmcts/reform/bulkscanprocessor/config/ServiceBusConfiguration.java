package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.Map;

import static uk.gov.hmcts.reform.bulkscanprocessor.config.ServiceBusQueueProperties.Queue;

@Lazy
@Configuration
@EnableConfigurationProperties(ServiceBusQueueProperties.class)
public class ServiceBusConfiguration {

    public static final String ENVELOPE_QUEUE_PUSH = "envelope-queue-push";
    public static final String NOTIFICATION_QUEUE_PUSH = "notification-queue-push";

    private final Map<ServiceBusQueues, Queue> queues;

    @Autowired
    private ObjectMapper mapper;

    public ServiceBusConfiguration(ServiceBusQueueProperties properties) {
        queues = properties.getMappings();
    }

    @Bean(name = ENVELOPE_QUEUE_PUSH)
    public ServiceBusHelper envelopesQueueHelper() throws InterruptedException, ServiceBusException {
        return getServiceBusHelper(
            getQueueClient(queues.get(ServiceBusQueues.ENVELOPES_PUSH))
        );
    }

    @Bean(name = NOTIFICATION_QUEUE_PUSH)
    public ServiceBusHelper notificationsQueueHelper() throws InterruptedException, ServiceBusException {
        return getServiceBusHelper(
            getQueueClient(queues.get(ServiceBusQueues.NOTIFICATIONS_PUSH))
        );
    }

    private IQueueClient getQueueClient(Queue queue) throws InterruptedException, ServiceBusException {
        return new QueueClient(
            new ConnectionStringBuilder(
                queue.getConnectionString(),
                queue.getQueueName()
            ),
            ReceiveMode.PEEKLOCK
        );
    }

    private ServiceBusHelper getServiceBusHelper(IQueueClient queueClient) {
        return new ServiceBusHelper(queueClient, mapper);
    }
}
