package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class QueueClientConfig {

    @Value("${queues.default-namespace}")
    private String defaultNamespace;

    @Bean("envelopes-config")
    @ConfigurationProperties(prefix = "queues.envelopes")
    protected QueueConfigurationProperties envelopesQueueConfig() {
        return new QueueConfigurationProperties();
    }

    @Bean("envelopes-client")
    public IQueueClient envelopesQueueClient(
        @Qualifier("envelopes-config") QueueConfigurationProperties queueProperties
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(queueProperties);
    }

    @Bean("processed-envelopes-config")
    @ConfigurationProperties(prefix = "queues.processed-envelopes")
    protected QueueConfigurationProperties processedEnvelopesQueueConfig() {
        return new QueueConfigurationProperties();
    }

    @Bean("processed-envelopes-client")
    public IQueueClient processedEnvelopesQueueClient(
        @Qualifier("processed-envelopes-config") QueueConfigurationProperties queueProperties
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(queueProperties);
    }

    @Bean("notifications-config")
    @ConfigurationProperties(prefix = "queues.notifications")
    protected QueueConfigurationProperties notificationsQueueConfig() {
        return new QueueConfigurationProperties();
    }

    @Bean("notifications-send-client")
    public ServiceBusSenderClient notificationsQueueClient(
        @Qualifier("notifications-config") QueueConfigurationProperties queueProperties
    ) {
        return createSendClient(queueProperties);
    }

    private ServiceBusSenderClient createSendClient(
        QueueConfigurationProperties queueProperties
    ) {
        String connectionString = String.format(
            "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            queueProperties.getNamespaceOverride().orElse(defaultNamespace),
            queueProperties.getAccessKeyName(),
            queueProperties.getAccessKey()
        );

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueProperties.getQueueName())
            .buildClient();

    }

    private QueueClient createQueueClient(
        QueueConfigurationProperties queueProperties
    ) throws ServiceBusException, InterruptedException {
        return new QueueClient(
            new ConnectionStringBuilder(
                queueProperties.getNamespaceOverride().orElse(defaultNamespace),
                queueProperties.getQueueName(),
                queueProperties.getAccessKeyName(),
                queueProperties.getAccessKey()
            ),
            ReceiveMode.PEEKLOCK
        );
    }
}
