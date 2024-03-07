package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessedEnvelopeNotificationHandler;

@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
@ConditionalOnExpression("!${jms.enabled}")
public class QueueClientConfig {
    public static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);
    public static final String CONNECTION_STR_FORMAT =
        "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;";

    @Value("${queues.default-namespace}")
    private String defaultNamespace;

    @Bean("envelopes-config")
    @ConfigurationProperties(prefix = "queues.envelopes")
    protected QueueConfigurationProperties envelopesQueueConfig() {
        return new QueueConfigurationProperties();
    }

    @Bean("envelopes-send-client")
    public ServiceBusSenderClient envelopesQueueClient(
        @Qualifier("envelopes-config") QueueConfigurationProperties queueProperties
    ) {
        return createSendClient(queueProperties);
    }

    @Bean("processed-envelopes-config")
    @ConfigurationProperties(prefix = "queues.processed-envelopes")
    protected QueueConfigurationProperties processedEnvelopesQueueConfig() {
        return new QueueConfigurationProperties();
    }

    @Bean("processed-envelopes-client")
    public ServiceBusProcessorClient processedEnvelopesQueueClient(
        @Qualifier("processed-envelopes-config") QueueConfigurationProperties queueProperties,
        ProcessedEnvelopeNotificationHandler messageHandler
    ) {
        return new ServiceBusClientBuilder()
            .connectionString(createConnectionString(queueProperties))
            .processor()
            .queueName(queueProperties.getQueueName())
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .disableAutoComplete()
            .processMessage(messageHandler::processMessage)
            .processError(messageHandler::processException)
            .buildProcessorClient();
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
        return new ServiceBusClientBuilder()
            .connectionString(createConnectionString(queueProperties))
            .sender()
            .queueName(queueProperties.getQueueName())
            .buildClient();

    }

    private String createConnectionString(QueueConfigurationProperties queueProperties) {
        return String.format(
            CONNECTION_STR_FORMAT,
            queueProperties.getNamespaceOverride().orElse(defaultNamespace),
            queueProperties.getAccessKeyName(),
            queueProperties.getAccessKey()
        );
    }
}
