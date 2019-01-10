package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ErrorNotificationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationExceptionHandler.class);

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
            log.error("Unknown exception", throwable);

            return autoCompletor.deadLetterAsync(lockToken, "Unknown exception", throwable.getMessage());
        }
    }

    private CompletableFuture<Void> handleErrorNotificationException(
        UUID lockToken,
        ErrorNotificationException exception
    ) {
        if (exception.getStatus().is5xxServerError()) {
            log.warn("Received server error from notification client", exception);
            // do nothing. wait for lock to expire
            return CompletableFuture.completedFuture(null);
        } else {
            log.error("Client error", exception);

            return autoCompletor.deadLetterAsync(lockToken, "Client error", exception.getMessage());
        }
    }
}
