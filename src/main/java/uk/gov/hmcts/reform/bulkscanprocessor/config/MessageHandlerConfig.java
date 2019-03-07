package uk.gov.hmcts.reform.bulkscanprocessor.config;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.MessageHandlerOptions;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ErrorNotificationHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.ProcessedEnvelopeNotificationHandler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

    @Value("${queues.read-notifications.connection-string}")
    private String readNotificationsConnectionString;

    @PostConstruct()
    @ConditionalOnProperty(value = "message-handlers.delay-registration", havingValue = "false", matchIfMissing = true)
    public void registerMessageHandlers() throws InterruptedException, ServiceBusException {
        log.info("Started registering message handlers (at application startup)");
        registerHandlers();
        log.info("Completed registering message handlers (at application startup)");
    }

    // In preview environment the service has to be alive before queues can be created. However,
    // the service itself needs to connect to those queues in order to register a message handler.
    // Because of this two-way dependency, message handlers can only be registered when the service
    // is already running (i.e. context has been loaded). There's no guarantee that queues will exists
    // from the very start, so the service has to wait for them.
    @EventListener
    @ConditionalOnProperty(value = "message-handlers.delay-registration")
    public void onApplicationContextRefreshed(
        ContextRefreshedEvent event
    ) throws ServiceBusException, InterruptedException {

        log.info("Started registering message handlers (after application startup)");

        waitForQueuesToExist();
        registerHandlers();

        log.info("Completed registering message handlers (after application startup)");
    }

    private void waitForQueuesToExist() throws ServiceBusException {
        final List<String> queueConnectionStrings = ImmutableList.of(
            processedEnvelopesConnectionString,
            readNotificationsConnectionString
        );

        log.info("Started waiting for queues to exist");

        for (String connectionString : queueConnectionStrings) {
            waitForQueueToExist(connectionString);
        }

        log.info("Finished waiting for queues to exist");
    }

    /**
     * Waits for a given queue to be present.
     *
     * <p>
     * Queue clients don't support message handler registration retries,
     * so the presence of the queue needs to be checked before the attempt is made.
     * </p>
     */
    private void waitForQueueToExist(String queueConnectionString) throws ServiceBusException {
        int attemptsLeft = 180;

        while (attemptsLeft-- > 0) {
            IMessageReceiver receiver = null;

            try {
                receiver = ClientFactory.createMessageReceiverFromConnectionString(queueConnectionString);
                attemptsLeft = 0;
            } catch (Exception e) {
                if (attemptsLeft == 0) {
                    throw new FailedToRegisterMessageHandlersException(
                        "Timed out trying to connect to Service Bus queue",
                        e
                    );
                } else {
                    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                }
            } finally {
                if (receiver != null) {
                    receiver.close();
                }
            }
        }
    }

    private void registerHandlers() throws ServiceBusException, InterruptedException {
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
            throwHandlerRegisrtationException(e);
        } catch (ServiceBusException e) {
            throwHandlerRegisrtationException(e);
        }
    }

    private void throwHandlerRegisrtationException(Exception cause) {
        throw new FailedToRegisterMessageHandlersException(
            "An error occurred when trying to register message handlers",
            cause
        );
    }
}
