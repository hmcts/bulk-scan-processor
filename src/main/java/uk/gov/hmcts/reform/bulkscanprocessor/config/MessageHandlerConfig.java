package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.google.common.util.concurrent.Uninterruptibles;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.MessagingEntityNotFoundException;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MessageHandlerConfig.class);
    private static final int MAX_REGISTRATION_ATTEMPTS = 5;

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
        boolean registered = false;

        for (int attemptNumber = 1; attemptNumber <= MAX_REGISTRATION_ATTEMPTS && !registered; attemptNumber++) {
            try {
                processedEnvelopesQueueClient.registerMessageHandler(
                    processedEnvelopeNotificationHandler,
                    messageHandlerOptions,
                    processedEnvelopesReadExecutor
                );

                registered = true;
            } catch (MessagingEntityNotFoundException e) {
                if (attemptNumber < MAX_REGISTRATION_ATTEMPTS) {
                    log.warn("Failed to register processed envelopes queue handler. Attempt {}", attemptNumber, e);
                    Uninterruptibles.sleepUninterruptibly(10L * attemptNumber, TimeUnit.SECONDS);
                } else {
                    throw e;
                }
            }
        }
    }
}
