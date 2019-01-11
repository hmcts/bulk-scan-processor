package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Functions;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers.ErrorNotificationExceptionHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ErrorNotificationHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationHandler.class);

    private final ErrorNotificationService service;

    private final ObjectMapper mapper;

    private final ErrorNotificationExceptionHandler exceptionHandler;

    private static final Executor SIMPLE_EXEC = Runnable::run;

    private static final Executor SERVICE_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "error-notification-service")
    );

    private static final Executor ERROR_EXEC = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "error-notification-handler")
    );

    public ErrorNotificationHandler(
        ErrorNotificationService service,
        ObjectMapper mapper,
        MessageAutoCompletor autoCompletor
    ) {
        this.service = service;
        this.mapper = mapper;
        this.exceptionHandler = new ErrorNotificationExceptionHandler(autoCompletor);
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage message) {
        return CompletableFuture
            .supplyAsync(message::getBody, SIMPLE_EXEC)
            .thenApplyAsync(this::getErrorMessage, SIMPLE_EXEC)
            .thenAcceptAsync(this::processMessage, SERVICE_EXEC)
            .handleAsync((voided, throwable) -> exceptionHandler.handle(message, throwable), ERROR_EXEC)
            .thenComposeAsync(Functions.identity(), ERROR_EXEC)
            .thenRunAsync(() -> log.debug("Error notification consumed. ID {}", message.getMessageId()), SIMPLE_EXEC);
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

    @Override
    public void notifyException(Throwable exception, ExceptionPhase phase) {
        log.error("Exception occurred in phase {}", phase, exception);
    }
}
