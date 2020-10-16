package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class QueueClientConfig {

    public static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);

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

    @Bean("notifications-client")
    public IQueueClient notificationsQueueClient(
        @Qualifier("notifications-config") QueueConfigurationProperties queueProperties
    ) throws InterruptedException, ServiceBusException {
        return createQueueClient(queueProperties);
    }

    private QueueClient createQueueClient(
        QueueConfigurationProperties queueProperties
    ) throws ServiceBusException, InterruptedException {
        log.info("getAccessKey: {}", queueProperties.getAccessKey());
        log.info("getQueueName :{}", queueProperties.getQueueName());
        log.info("getAccessKeyName: {}", queueProperties.getAccessKeyName());
        // once notification secrets are set this ternary can be removed
        var connectionStringBuilder = StringUtils.isEmpty(queueProperties.getAccessKey())
            ? new ConnectionStringBuilder(queueProperties.getConnectionString(), queueProperties.getQueueName())
            : new ConnectionStringBuilder(
                queueProperties.getNamespaceOverride().orElse(defaultNamespace),
                queueProperties.getQueueName(),
                queueProperties.getAccessKeyName(),
                queueProperties.getAccessKey()
            );

        return new QueueClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
    }
}
