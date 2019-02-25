package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Value("${FAIL_ON_MESSAGE_HANDLER_REGISTRATION_ERROR:true}")
    private boolean failOnMessageHandlerRegistrationError;

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
            handleMessageHandlerRegistrationError(e);
        } catch (ServiceBusException e) {
            handleMessageHandlerRegistrationError(e);
        }
    }

    private <T extends Exception> void handleMessageHandlerRegistrationError(T cause) throws T {
        final String errorMessage = "An error occurred when trying to register message handlers";

        if (failOnMessageHandlerRegistrationError) {
            throw cause;
        } else {
            // The application has to keep working on Preview - otherwise the pipeline
            // wouldn't create queues which it relies on.
            // The problem will be addresses in BPS-445
            log.error(errorMessage, cause);
        }
    }
}
