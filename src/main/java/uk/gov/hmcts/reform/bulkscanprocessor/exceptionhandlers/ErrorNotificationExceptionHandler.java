package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;

import java.time.Instant;
import javax.validation.constraints.NotNull;

public class ErrorNotificationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationExceptionHandler.class);

    private final IQueueClient errorNotificationPushClient;

    public ErrorNotificationExceptionHandler(IQueueClient errorNotificationPushClient) {
        this.errorNotificationPushClient = errorNotificationPushClient;
    }

    public IMessage handle(IMessage message, Throwable throwable) {
        // return for logging part. and future requires something to be returned other than void
        return throwable != null ? handleNonNullThrowable(message, throwable) : message;
    }

    private IMessage handleNonNullThrowable(IMessage message, @NotNull Throwable throwable) {
        try {
            throw (RuntimeException) throwable;
        } catch (ClassCastException exception) {
            log.error("Unable to cast Throwable to RuntimeException", throwable);

            throw exception;
        } catch (ErrorNotificationException exception) {
            return handleErrorNotificationException(message, exception);
        }
    }

    private IMessage handleErrorNotificationException(IMessage message, ErrorNotificationException exception) {
        if (exception.getStatus().is5xxServerError()) {
            // 10th delivery time is 6h delay
            // Eventually it'll fail in case client is down for quite some time
            long secondsToAdd = (long) Math.floor(Math.exp((double) message.getDeliveryCount()));

            errorNotificationPushClient.scheduleMessageAsync(message, Instant.now().plusSeconds(secondsToAdd));

            return message;
        }

        throw exception;
    }
}
