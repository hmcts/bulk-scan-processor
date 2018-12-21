package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;

public class ErrorNotificationExceptionHandler {

    private final IQueueClient errorNotificationPushClient;

    public ErrorNotificationExceptionHandler(IQueueClient errorNotificationPushClient) {
        this.errorNotificationPushClient = errorNotificationPushClient;
    }

    public IMessage handle(IMessage message, Throwable throwable) {
        if (throwable != null) {
            // decide whether to dead-letter (throw exc) or re-schedule the message.
            throw (RuntimeException) throwable;
        } else {
            return message;
        }
        // return for logging part. and future requires something to be returned other than void
    }
}
