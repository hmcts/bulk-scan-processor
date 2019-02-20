package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.google.common.util.concurrent.Uninterruptibles;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessedEnvelopeNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;

@ServiceBusConfiguration
public class MessageHandlerConfig {

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

    @PostConstruct
    public void registerMessageHandlers() throws ServiceBusException, InterruptedException {
        if (readNotificationsQueueClient != null) {
            readNotificationsQueueClient.registerMessageHandler(
                errorNotificationHandler,
                messageHandlerOptions,
                notificationsReadExecutor
            );
        }

        registerProcessedEnvelopeNotificationHandler();
    }

    private void registerProcessedEnvelopeNotificationHandler() throws ServiceBusException, InterruptedException {
        try {
            processedEnvelopesQueueClient.registerMessageHandler(
                processedEnvelopeNotificationHandler,
                messageHandlerOptions,
                processedEnvelopesReadExecutor
            );
        } catch (MessagingEntityNotFoundException e) {
            // This is when the queue doesn't exist, yet (can be a temporary situation in AKS)
            // Can't recover from this, as the client sets handler registered flag at the beginning of the process
            // and won't allow for another registration attempt.
            // Let the app work for a bit, so that its health endpoint can respond -
            // otherwise the (AKS) pipeline will never create the queue that is needed for the app to run.
            Uninterruptibles.sleepUninterruptibly(60L, TimeUnit.SECONDS);
            throw e;
        }
    }
}
