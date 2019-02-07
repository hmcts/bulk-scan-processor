package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@Lazy
@Configuration
@AutoConfigureAfter(QueueClientConfig.class)
@Profile("!nosb") // no servicebus queue handler registration
public class ServiceBusConfiguration {

    private static final ExecutorService NOTIFICATIONS_READ_EXEC =
        Executors.newSingleThreadExecutor(r ->
            new Thread(r, "notifications-queue-read")
        );

    @Autowired(required = false)
    @Qualifier("read-notifications")
    private IQueueClient readNotificationsQueueClient;

    @Autowired
    private ErrorNotificationHandler errorNotificationHandler;

    @Autowired
    private ObjectMapper objectMapper;


    @Bean(name = "envelopes-helper")
    public ServiceBusHelper envelopesQueueHelper(
        @Qualifier("envelopes") IQueueClient queueClient
    ) {
        return new ServiceBusHelper(queueClient, objectMapper);
    }

    @Bean(name = "notifications-helper")
    public ServiceBusHelper notificationsQueueHelper(
        @Qualifier("notifications") IQueueClient queueClient
    ) {
        return new ServiceBusHelper(queueClient, objectMapper);
    }

    @Bean(name = "read-notifications-completor")
    public MessageAutoCompletor readNotificationsMessageCompletor(
        @Qualifier("read-notifications") IQueueClient queueClient
    ) {
        return new MessageAutoCompletor(queueClient);
    }

    @PostConstruct
    public void registerMessageHandlers() throws ServiceBusException, InterruptedException {
        if (readNotificationsQueueClient != null) {
            readNotificationsQueueClient.registerMessageHandler(
                errorNotificationHandler,
                new MessageHandlerOptions(1, false, Duration.ofMinutes(5)),
                NOTIFICATIONS_READ_EXEC
            );
        }
    }
}
