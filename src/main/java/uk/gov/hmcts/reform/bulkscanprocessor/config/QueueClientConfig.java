package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class QueueClientConfig {

    @Value("${test.sb.conn-string}")
    private String connString;
    @Value("${test.sb.access-key}")
    private String accessKey;
    @Value("${test.sb.namespace}")
    private String namespace;

    @Bean("envelopes-client")
    public IQueueClient envelopesQueueClient(
        @Value("${queues.envelopes.connection-string}") String connectionString,
        @Value("${queues.envelopes.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(connectionString, queueName);
    }

    @Bean("processed-envelopes-client")
    public IQueueClient processedEnvelopesQueueClient(
        @Value("${queues.processed-envelopes.connection-string}") String connectionString,
        @Value("${queues.processed-envelopes.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(connectionString, queueName);
    }

    @Bean("notifications-client")
    public IQueueClient notificationsQueueClient(
        @Value("${queues.notifications.connection-string}") String connectionString,
        @Value("${queues.notifications.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(connectionString, queueName);
    }

    private QueueClient createQueueClient(
        String connectionString,
        String queueName
    ) throws ServiceBusException, InterruptedException {
        return new QueueClient(
            new ConnectionStringBuilder(connectionString, queueName),
            ReceiveMode.PEEKLOCK
        );
    }

    @PostConstruct
    private void init() {
        var log = org.slf4j.LoggerFactory.getLogger(QueueClientConfig.class);
        log.warn("Connection String: {}", connString);
        log.warn("Access Key: {}", accessKey);
        log.warn("Namespace: {}", namespace);
    }
}
