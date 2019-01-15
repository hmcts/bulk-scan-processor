package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessageHandler;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@Lazy
@Configuration
public class ServiceBusConfiguration {

    private static final ExecutorService QUEUE_READ_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "notifications-queue-read")
    );

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ErrorNotificationService errorNotificationService;

    @Value("${queues.read-notifications.enabled}")
    private boolean readNotificationsEnabled;

    @Value("${queues.read-notifications.connection-string}")
    private String readNotificationConnectionString;

    @Value("${queues.read-notifications.queue-name}")
    private String readNotificationQueueName;

    @Bean(name = "envelopes")
    public ServiceBusHelper envelopesQueueHelper(
        @Value("${queues.envelopes.connection-string}") String connectionString,
        @Value("${queues.envelopes.queue-name}") String queueName
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
        @Value("${queues.notifications.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return new ServiceBusHelper(
            new QueueClient(
                new ConnectionStringBuilder(connectionString, queueName),
                ReceiveMode.PEEKLOCK
            ),
            objectMapper
        );
    }

    @PostConstruct
    public void notificationsQueueReader() throws InterruptedException, ServiceBusException {
        if (readNotificationsEnabled) {
            IQueueClient client = new QueueClient(
                new ConnectionStringBuilder(readNotificationConnectionString, readNotificationQueueName),
                ReceiveMode.PEEKLOCK
            );

            IMessageHandler messageHandler = new ErrorNotificationHandler(
                errorNotificationService,
                objectMapper,
                new MessageAutoCompletor(client)
            );

            client.registerMessageHandler(
                messageHandler,
                new MessageHandlerOptions(1, false, Duration.ofMinutes(5)),
                QUEUE_READ_EXEC
            );
        }
    }
}
