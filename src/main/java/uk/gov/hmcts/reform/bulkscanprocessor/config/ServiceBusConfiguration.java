package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.QueueClient;

import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfiguration {

    @Bean
    public QueueClient getQueueClient(
        @Value("${servicebus.queue_envelope_send}") String connectionString
    ) throws ServiceBusException, InterruptedException {

        return new QueueClient(
            new ConnectionStringBuilder(connectionString),
            ReceiveMode.PEEKLOCK
        );
    }

}
