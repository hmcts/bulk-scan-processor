package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ErrorNotificationMessageWrapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorNotificationExceptionHandlerTest {

    private static final IMessage MESSAGE = new Message("content");

    private static final ErrorNotificationExceptionHandler HANDLER = new ErrorNotificationExceptionHandler();

    @Test
    public void should_mark_for_acknowledgement_when_no_exception_sent_to_handle() {
        ErrorNotificationMessageWrapper wrapper = HANDLER.handle(MESSAGE, null);

        assertThat(wrapper.isAssignedForDeadLettering()).isFalse();
        assertThat(wrapper.isCompletedAcknowledgement()).isTrue();
    }

    @Test
    public void should_mark_for_deadletter_when_exception_is_not_runtime_exception() {
        ErrorNotificationMessageWrapper wrapper = HANDLER.handle(MESSAGE, new IOException("oh no"));

        assertThat(wrapper.isAssignedForDeadLettering()).isTrue();
        assertThat(wrapper.isCompletedAcknowledgement()).isFalse();
    }

    @Test
    public void should_mark_for_deadletter_when_exception_is_4xx_of_notification_exception() {
        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {},
            null
        );
        ErrorNotificationMessageWrapper wrapper = HANDLER.handle(MESSAGE, exception);

        assertThat(wrapper.isAssignedForDeadLettering()).isTrue();
        assertThat(wrapper.isCompletedAcknowledgement()).isFalse();
    }

    @Test
    public void should_mark_for_abandonment_when_exception_is_5xx_of_notification_exception() {
        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {},
            null
        );
        ErrorNotificationMessageWrapper wrapper = HANDLER.handle(MESSAGE, exception);

        assertThat(wrapper.isAssignedForDeadLettering()).isFalse();
        assertThat(wrapper.isCompletedAcknowledgement()).isFalse();
    }
}
