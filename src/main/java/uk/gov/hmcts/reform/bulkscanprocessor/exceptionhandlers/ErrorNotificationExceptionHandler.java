package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ErrorNotificationMessageWrapper;

import javax.validation.constraints.NotNull;

public class ErrorNotificationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorNotificationExceptionHandler.class);

    public ErrorNotificationExceptionHandler() {
        // empty construct
    }

    public ErrorNotificationMessageWrapper handle(IMessage message, Throwable throwable) {
        // return for message session completion step
        return throwable != null
            ? handleNonNullThrowable(message, throwable)
            : ErrorNotificationMessageWrapper.forAcknowledgement(message);
    }

    private ErrorNotificationMessageWrapper handleNonNullThrowable(IMessage message, @NotNull Throwable throwable) {
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
