package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ErrorNotificationExceptionHandlerTest {

    @Mock
    private IQueueClient queueClient;

    private ErrorNotificationExceptionHandler handler;

    @Before
    public void setUp() {
        handler = new ErrorNotificationExceptionHandler(queueClient);
    }

    @Test
    public void should_return_same_message_in_case_no_error_to_be_handled() {
        // given
        Message message = new Message("content");

        // when
        IMessage handledMessage = handler.handle(message, null);

        // then
        assertThat(handledMessage).isEqualTo(message);
    }

    @Test
    public void should_handle_non_RuntimeException_cases_by_catching_the_cast() {
        Throwable throwable = catchThrowable(() -> handler.handle(null, new IOException("oh no")));

        assertThat(throwable).isInstanceOf(ClassCastException.class);
    }

    @Test
    public void should_handle_ErrorNotificationException_with_4xx_error() {
        // given
        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {},
            null
        );

        // when
        Throwable throwable = catchThrowable(() -> handler.handle(null, exception));

        // then
        assertThat(throwable).isEqualTo(exception);

        // and
        verify(queueClient, never()).scheduleMessageAsync(any(IMessage.class), any(Instant.class));
    }

    @Test
    public void should_reschedule_message_in_case_ErrorNotificationException_is_with_5xx_error() {
        // given
        Message message = new Message("content");
        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {},
            null
        );

        // when
        IMessage handledMessage = handler.handle(message, exception);

        // then
        assertThat(handledMessage).isEqualTo(message);

        // and
        verify(queueClient).scheduleMessageAsync(eq(message), any(Instant.class));
    }
}
