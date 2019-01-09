package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.concurrent.CompletableFuture;

import static uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor.DeadLetterReason;

public class ErrorNotificationExceptionHandler {

    private final MessageAutoCompletor autoCompletor;

    public ErrorNotificationExceptionHandler(MessageAutoCompletor autoCompletor) {
        this.autoCompletor = autoCompletor;
    }

    public CompletableFuture<Void> handle(IMessage message, Throwable throwable) {
        // return for message session completion step
        return throwable != null
            ? handleNonNullThrowable(message, throwable)
            : autoCompletor.completeAsync(message.getLockToken());
    }

    private CompletableFuture<Void> handleNonNullThrowable(IMessage message, Throwable throwable) {
        if (throwable instanceof ErrorNotificationException) {
            return handleErrorNotificationException(message, (ErrorNotificationException) throwable);
        } else {
            return autoCompletor.deadLetterAsync(
                message.getLockToken(),
                new DeadLetterReason("Unknown exception", throwable.getMessage())
            );
        }
    }

    private CompletableFuture<Void> handleErrorNotificationException(
        IMessage message,
        ErrorNotificationException exception
    ) {
        if (exception.getStatus().is5xxServerError()) {
            return autoCompletor.abandonAsync(message.getLockToken());
        } else {
            return autoCompletor.deadLetterAsync(
                message.getLockToken(),
                new DeadLetterReason("Client error", exception.getMessage())
            );
        }
    }
}
