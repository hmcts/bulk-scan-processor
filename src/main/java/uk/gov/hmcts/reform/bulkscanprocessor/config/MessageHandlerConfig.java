package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessedEnvelopeNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;

@AutoConfigureAfter(ServiceBusHelpersConfiguration.class)
@Configuration
@Profile(Profiles.NOT_SERVICE_BUS_STUB)
public class MessageHandlerConfig {

    public static final Logger log = LoggerFactory.getLogger(MessageHandlerConfig.class);

    private static final ExecutorService notificationsReadExecutor =
        Executors.newSingleThreadExecutor(r ->
            new Thread(r, "notifications-queue-read")
        );

    private static final ExecutorService processedEnvelopesReadExecutor =
        Executors.newSingleThreadExecutor(r ->
            new Thread(r, "processed-envelopes-queue-read")
        );

    private static final MessageHandlerOptions messageHandlerOptions =
        new MessageHandlerOptions(1, false, Duration.ofMinutes(5));

    @Autowired(required = false)
    @Qualifier("read-notifications-client")
    private IQueueClient readNotificationsQueueClient;

    @Autowired
    @Qualifier("processed-envelopes-client")
    private IQueueClient processedEnvelopesQueueClient;

    @Autowired(required = false)
    private ErrorNotificationHandler errorNotificationHandler;

    @Autowired
    private ProcessedEnvelopeNotificationHandler processedEnvelopeNotificationHandler;

    @PostConstruct()
    public void registerMessageHandlers() throws InterruptedException, ServiceBusException {
        if (readNotificationsQueueClient != null) {
            readNotificationsQueueClient.registerMessageHandler(
                errorNotificationHandler,
                messageHandlerOptions,
                notificationsReadExecutor
            );
        }

        processedEnvelopesQueueClient.registerMessageHandler(
            processedEnvelopeNotificationHandler,
            messageHandlerOptions,
            processedEnvelopesReadExecutor
        );
    }
}
