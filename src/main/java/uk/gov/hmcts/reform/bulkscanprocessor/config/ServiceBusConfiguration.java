package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class ServiceBusConfiguration {

    @Bean
    public QueueClient queueClient(
        @Value("${azure.servicebus.connection-string}") String connectionString,
        @Value("${azure.servicebus.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return new QueueClient(
            new ConnectionStringBuilder(connectionString, queueName),
            ReceiveMode.PEEKLOCK
        );
    }
}
