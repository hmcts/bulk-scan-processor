package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

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
        Throwable processedThrowable = processThrowable(throwable);

        if (processedThrowable instanceof ErrorNotificationException) {
            return handleErrorNotificationException(
                lockToken,
                messageId,
                deliveryCount,
                (ErrorNotificationException) processedThrowable
            );
        } else {
            log.error(
                "Unknown exception. Dead lettering message (ID: {})",
                messageId,
                processedThrowable
            );

            return autoCompletor.deadLetterAsync(lockToken, "Unknown exception", processedThrowable.getMessage());
        }
    }

    /**
     * {@link CompletableFuture} wraps all exceptions with {@link CompletionException}. We are interested in the cause
     * @param throwable A {@link CompletionException} instance
     * @return Cause of {@link Throwable} or itself
     */
    private Throwable processThrowable(Throwable throwable) {
        return throwable instanceof CompletionException ? throwable.getCause() : throwable;
    }

    private CompletableFuture<Void> handleErrorNotificationException(
        UUID lockToken,
        String messageId,
        long deliveryCount,
        ErrorNotificationException exception
    ) {
        logExtraInformationAboutResponse(exception);

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

    /**
     * Perform additional log about response received from notification client.
     * Feign clients do not provide feature of Response interceptors so it has to be dealt manually.
     * @param exception Decoded notification exception
     * @see uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationDecoder
     */
    private void logExtraInformationAboutResponse(ErrorNotificationException exception) {
        StringBuilder builder = new StringBuilder("Error occurred when posting notification");
        boolean toLog = false;

        if (exception.getResponse() != null) {
            toLog = true;
            builder
                .append(". Parsed response: ")
                .append(exception.getResponse().getMessage());
        }

        if (!StringUtils.isEmpty(exception.getResponseRawBody())) {
            toLog = true;
            builder
                .append(". Raw response: ")
                .append(exception.getResponseRawBody());
        }

        if (toLog) {
            log.info(builder.toString());
        }
    }
}
