package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(QueueConfigurationProperties.class)
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class QueueClientConfig implements ImportBeanDefinitionRegistrar {

    QueueConfigurationProperties queueConfigurations;

    public QueueClientConfig(QueueConfigurationProperties queueConfigurations) {
        this.queueConfigurations = queueConfigurations;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        queueConfigurations.getQueues().forEach((queuePrefix, queueItem) -> {
            // once notification secrets are set this ternary can be removed
            var connectionStringBuilder = StringUtils.isEmpty(queueItem.getAccessKey())
                ? new ConnectionStringBuilder(queueItem.getConnectionString())
                : new ConnectionStringBuilder(
                    queueItem.getNamespaceOverride().orElse(queueConfigurations.getDefaultNamespace()),
                    queueItem.getQueueName(),
                    queueItem.getAccessKeyName(),
                    queueItem.getAccessKey()
                );

            register(registry, connectionStringBuilder, queuePrefix);
        });
    }

    private void register(
        BeanDefinitionRegistry registry,
        ConnectionStringBuilder connectionStringBuilder,
        String beanPrefix
    ) {
        BeanDefinition beanDefinition = new RootBeanDefinition(IQueueClient.class, () -> {
            try {
                return new QueueClient(connectionStringBuilder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException exception) {
                Thread.interrupted();
                throw new RuntimeException("Unable to create QueueClient for " + beanPrefix, exception);
            } catch (ServiceBusException exception) {
                throw new RuntimeException("Unable to create QueueClient for " + beanPrefix, exception);
            }
        });

        registry.registerBeanDefinition(beanPrefix + "-client", beanDefinition);
    }
}
