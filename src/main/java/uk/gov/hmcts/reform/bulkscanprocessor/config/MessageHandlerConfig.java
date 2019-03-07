package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
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

    @Value("${queues.processed-envelopes.connection-string}")
    private String processedEnvelopesConnectionString;

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleMessageHandlerRegistrationError(e);
        } catch (ServiceBusException e) {
            handleMessageHandlerRegistrationError(e);
        }
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Application context loaded - registering the message handler");

        boolean queueExists = waitForQueue();

        if (queueExists) {
            try {
                processedEnvelopesQueueClient.registerMessageHandler(
                    processedEnvelopeNotificationHandler,
                    messageHandlerOptions,
                    processedEnvelopesReadExecutor
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to register message handler");
            } catch (ServiceBusException e) {
                log.error("Failed to register message handler");
            }
        }
    }

    private boolean waitForQueue() {
        int attemptsLeft = 100;
        boolean queueExists = false;

        while (!queueExists && attemptsLeft-- > 0) {
            try {
                ClientFactory.createMessageReceiverFromConnectionString(
                    processedEnvelopesConnectionString
                ).peek();

                queueExists = true;
                log.info("Queue exists");
            } catch (Exception e) {
                log.info("Queue still doesn't exist");
            }
        }

        if (!queueExists) {
            log.error("Queue wasn't created within time limit");
        }

        return queueExists;
    }

    private void registerProcessedEnvelopeMessageHandler() {
        try {
            processedEnvelopesQueueClient.registerMessageHandler(
                processedEnvelopeNotificationHandler,
                messageHandlerOptions,
                processedEnvelopesReadExecutor
            );
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ServiceBusException e) {
            e.printStackTrace();
        }
    }

    private <T extends Exception> void handleMessageHandlerRegistrationError(T cause) throws T {
        if (failOnMessageHandlerRegistrationError) {
            throw cause;
        } else {
            // The application has to keep working on Preview - otherwise the pipeline
            // wouldn't create queues which it relies on.
            // The problem will be addresses in BPS-445
            log.error("An error occurred when trying to register message handlers", cause);
        }
    }
}
