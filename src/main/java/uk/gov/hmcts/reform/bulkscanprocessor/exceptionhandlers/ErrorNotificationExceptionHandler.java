package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
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

    public CompletableFuture<Void> handle(IMessage message, Throwable throwable) {
        UUID lockToken = message.getLockToken();
        // return for message session completion step
        return throwable != null
            ? handleNonNullThrowable(lockToken, message.getMessageId(), message.getDeliveryCount(), throwable)
            : autoCompletor.completeAsync(lockToken);
    }

    private CompletableFuture<Void> handleNonNullThrowable(
        UUID lockToken,
        String messageId,
        long deliveryCount,
        Throwable throwable
    ) {
        if (throwable instanceof ErrorNotificationException) {
            return handleErrorNotificationException(
                lockToken,
                messageId,
                deliveryCount,
                (ErrorNotificationException) throwable
            );
        } else {
            log.error(
                "Unknown exception. Dead lettering message (ID: {})",
                messageId,
                throwable
            );

            return autoCompletor.deadLetterAsync(lockToken, "Unknown exception", throwable.getMessage());
        }
    }

    private CompletableFuture<Void> handleErrorNotificationException(
        UUID lockToken,
        String messageId,
        long deliveryCount,
        ErrorNotificationException exception
    ) {
        if (exception.getStatus().is5xxServerError()) {
            log.warn(
                "Received server error from notification client. Voiding message (ID: {}) after {} delivery attempt",
                messageId,
                deliveryCount + 1, // starts from 0
                exception
            );
            // do nothing. wait for lock to expire
            return CompletableFuture.completedFuture(null);
        } else {
            log.error(
                "Client error. Dead lettering message (ID: {})",
                messageId,
                exception
            );

            return autoCompletor.deadLetterAsync(lockToken, "Client error", exception.getMessage());
        }
    }
}
