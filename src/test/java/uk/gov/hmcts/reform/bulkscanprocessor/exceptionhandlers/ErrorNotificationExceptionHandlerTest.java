package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import com.microsoft.azure.servicebus.IMessageReceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ErrorNotificationExceptionHandlerTest {

    @Mock
    private IMessageReceiver receiver;

    private ErrorNotificationExceptionHandler handler;

    private static final UUID LOCKE_TOKEN = UUID.randomUUID();

    private static final CompletableFuture<Void> COMPLETED_FUTURE = CompletableFuture.completedFuture(null);

    @Before
    public void setUp() {
        handler = new ErrorNotificationExceptionHandler(new MessageAutoCompletor(receiver));
    }

    @Test
    public void should_mark_for_acknowledgement_when_no_exception_sent_to_handle() {
        given(receiver.completeAsync(any(UUID.class))).willReturn(COMPLETED_FUTURE);

        CompletableFuture<Void> handled = handler.handle(LOCKE_TOKEN, null);

        assertThat(handled.join()).isNull();
    }

    @Test
    public void should_mark_for_deadletter_when_exception_is_not_ErrorNotificationException() {
        given(receiver.deadLetterAsync(any(UUID.class), anyString(), anyString())).willReturn(COMPLETED_FUTURE);

        CompletableFuture<Void> handled = handler.handle(LOCKE_TOKEN, new IOException("oh no"));

        assertThat(handled.join()).isNull();
    }

    @Test
    public void should_mark_for_deadletter_when_exception_is_4xx_of_notification_exception() {
        given(receiver.deadLetterAsync(any(UUID.class), anyString(), anyString())).willReturn(COMPLETED_FUTURE);

        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.BAD_REQUEST) {},
            null
        );
        CompletableFuture<Void> handled = handler.handle(LOCKE_TOKEN, exception);

        assertThat(handled.join()).isNull();
    }

    @Test
    public void should_mark_for_abandonment_when_exception_is_5xx_of_notification_exception() {
        given(receiver.abandonAsync(any(UUID.class), any())).willReturn(COMPLETED_FUTURE);

        ErrorNotificationException exception = new ErrorNotificationException(
            new HttpStatusCodeException(HttpStatus.INTERNAL_SERVER_ERROR) {},
            null
        );
        CompletableFuture<Void> handled = handler.handle(LOCKE_TOKEN, exception);

        assertThat(handled.join()).isNull();
    }
}
