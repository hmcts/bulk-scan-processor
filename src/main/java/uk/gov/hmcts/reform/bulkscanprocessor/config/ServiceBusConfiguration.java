package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.QueueClient;

import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceBusConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusConfiguration.class);

    @Bean
    public QueueClient getQueueClient(
        @Value("${servicebus.queue_envelope_send}") String connectionString
    ) throws ServiceBusException, InterruptedException {

        log.info("Queue Client connection string = [{}]", connectionString);

        return new QueueClient(
            new ConnectionStringBuilder(connectionString),
            ReceiveMode.PEEKLOCK
        );
    }

}
