package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageSession;
import com.microsoft.azure.servicebus.ISessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ErrorNotificationHandler implements ISessionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationHandler.class);

    private final ErrorNotificationService service;

    private final ObjectMapper mapper;

    private static final Executor SIMPLE_EXEC = Runnable::run;

    private static final Executor SERVICE_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "error-notification-service")
    );

    private static final Executor SERVICEBUS_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "error-notification-servicebus")
    );


    public ErrorNotificationHandler(
        ErrorNotificationService service,
        ObjectMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessageSession session, IMessage message) {
        return CompletableFuture
            .supplyAsync(message::getBody, SIMPLE_EXEC)
            .thenApplyAsync(this::getErrorMessage, SIMPLE_EXEC)
            .thenAcceptAsync(this::processMessage, SERVICE_EXEC)
            .thenComposeAsync(
                aVoid -> acknowledgeMessage(session, message),
                SERVICEBUS_EXEC
            );
    }

    @Override
    public CompletableFuture<Void> OnCloseSessionAsync(IMessageSession session) {
        // nothing to oo
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void notifyException(Throwable exception, ExceptionPhase phase) {
        log.error("Exception occurred in phase {}", phase, exception);
    }

    private ErrorMsg getErrorMessage(byte[] message) {
        try {
            return mapper.readValue(message, ErrorMsg.class);
        } catch (IOException exception) {
            throw new InvalidMessageException("Unable to read error message", exception);
        }
    }

    private void processMessage(ErrorMsg message) {
        service.processServiceBusMessage(message);
    }

    private CompletableFuture<Void> acknowledgeMessage(IMessageSession session, IMessage message) {
        log.debug("Error notification consumed. ID {}", message.getMessageId());

        return session.completeAsync(message.getLockToken());
    }
}
