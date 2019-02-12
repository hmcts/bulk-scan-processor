package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@ServiceBusConfiguration
public class MessageHandlerConfig {

    private static final ExecutorService NOTIFICATIONS_READ_EXEC =
        Executors.newSingleThreadExecutor(r ->
            new Thread(r, "notifications-queue-read")
        );

    @Autowired(required = false)
    @Qualifier("read-notifications-client")
    private IQueueClient readNotificationsQueueClient;

    @Autowired(required = false)
    private ErrorNotificationHandler errorNotificationHandler;

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
