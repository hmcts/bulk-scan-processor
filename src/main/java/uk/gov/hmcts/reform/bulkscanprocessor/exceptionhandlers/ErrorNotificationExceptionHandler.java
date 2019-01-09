package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor.DeadLetterReason;

public class ErrorNotificationExceptionHandler {

    private final MessageAutoCompletor autoCompletor;

    public ErrorNotificationExceptionHandler(MessageAutoCompletor autoCompletor) {
        this.autoCompletor = autoCompletor;
    }

    public CompletableFuture<Void> handle(UUID lockToken, Throwable throwable) {
        // return for message session completion step
        return throwable != null
            ? handleNonNullThrowable(lockToken, throwable)
            : autoCompletor.completeAsync(lockToken);
    }

    private CompletableFuture<Void> handleNonNullThrowable(UUID lockToken, Throwable throwable) {
        if (throwable instanceof ErrorNotificationException) {
            return handleErrorNotificationException(lockToken, (ErrorNotificationException) throwable);
        } else {
            return autoCompletor.deadLetterAsync(
                lockToken,
                new DeadLetterReason("Unknown exception", throwable.getMessage())
            );
        }
    }

    private CompletableFuture<Void> handleErrorNotificationException(
        UUID lockToken,
        ErrorNotificationException exception
    ) {
        if (exception.getStatus().is5xxServerError()) {
            return autoCompletor.abandonAsync(lockToken);
        } else {
            return autoCompletor.deadLetterAsync(
                lockToken,
                new DeadLetterReason("Client error", exception.getMessage())
            );
        }
    }
}
