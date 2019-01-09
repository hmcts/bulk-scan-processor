package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ErrorNotificationMessageWrapper;

public class ErrorNotificationExceptionHandler {

    public ErrorNotificationExceptionHandler() {
        // empty construct
    }

    public ErrorNotificationMessageWrapper handle(IMessage message, Throwable throwable) {
        // return for message session completion step
        return throwable != null
            ? handleNonNullThrowable(message, throwable)
            : ErrorNotificationMessageWrapper.forAcknowledgement(message);
    }

    private ErrorNotificationMessageWrapper handleNonNullThrowable(IMessage message, Throwable throwable) {
        if (throwable instanceof ErrorNotificationException) {
            return handleErrorNotificationException(message, (ErrorNotificationException) throwable);
        } else {
            return ErrorNotificationMessageWrapper.forDeadLettering(message);
        }
    }

    private ErrorNotificationMessageWrapper handleErrorNotificationException(
        IMessage message,
        ErrorNotificationException exception
    ) {
        if (exception.getStatus().is5xxServerError()) {
            return ErrorNotificationMessageWrapper.forAbandoning(message);
        } else {
            return ErrorNotificationMessageWrapper.forDeadLettering(message);
        }
    }
}
