package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessedEnvelopeNotificationHandler;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Value("${WAIT_FOR_QUEUE_CREATION:false}")
    private boolean waitForQueueCreation;

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
    public void registerMessageHandlers() {
        if (waitForQueueCreation) {
            new Thread(() ->
                throwRegisterMessageHandlerRegistrationException(null)
            ).start();
        } else {
            registerHandlers();
        }
    }

    private void registerHandlers() {
        try {
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throwRegisterMessageHandlerRegistrationException(e);
        } catch (ServiceBusException e) {
            throwRegisterMessageHandlerRegistrationException(e);
        }
    }

    private void throwRegisterMessageHandlerRegistrationException(Exception cause) {
        throw new FailedToRegisterMessageHandlersExcepiton(
            "An error occurred when trying to register message handlers",
            cause
        );
    }
}
