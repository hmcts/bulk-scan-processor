package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;

@RunWith(MockitoJUnitRunner.class)
public class ErrorNotificationHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ErrorNotificationService service;

    @Mock
    private IQueueClient queueClient;

    private ErrorNotificationHandler handler;

    @Before
    public void setUp() {
        handler = new ErrorNotificationHandler(service, MAPPER, queueClient);
    }

    @Test
    public void should_fail_exceptionally_when_trying_parse_the_message_body() {
        // given
        IMessage message = getSampleMessage("{}".getBytes());

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);

        // then
        assertThat(future.isCompletedExceptionally()).isTrue();

        // and
        Throwable throwable = catchThrowable(future::join);
        assertThat(throwable.getCause())
            .isInstanceOf(InvalidMessageException.class)
            .hasMessageContaining("Unable to read error message");
        assertThat(throwable.getCause().getCause())
            .isInstanceOf(JsonProcessingException.class)
            .hasMessageContaining("Missing required creator property 'id'");

        // and
        verify(service, never()).processServiceBusMessage(any(ErrorMsg.class));
    }

    @Test
    public void should_fail_exceptionally_when_service_throws_an_error() throws JsonProcessingException {
        // given
        ErrorMsg msg = getSampleErrorMessage();
        IMessage message = getSampleMessage(MAPPER.writeValueAsBytes(msg));
        willThrow(new RuntimeException("oh no")).given(service).processServiceBusMessage(any(ErrorMsg.class));

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        Throwable throwable = catchThrowable(future::join);

        // then
        assertThat(future.isCompletedExceptionally()).isTrue();
        assertThat(throwable.getCause())
            .isInstanceOf(RuntimeException.class)
            .hasMessage("oh no");

        // and
        verify(service).processServiceBusMessage(any(ErrorMsg.class));
    }

    @Test
    public void should_complete_task_successfully() throws JsonProcessingException {
        // given
        ErrorMsg msg = getSampleErrorMessage();
        IMessage message = getSampleMessage(MAPPER.writeValueAsBytes(msg));
        doNothing().when(service).processServiceBusMessage(any(ErrorMsg.class));

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        verify(service).processServiceBusMessage(any(ErrorMsg.class));
    }

    private ErrorMsg getSampleErrorMessage() {
        return new ErrorMsg(
            "id",
            0L,
            "zip_file_name",
            "jurisdiction",
            "po_box",
            "document_control_number",
            ErrorCode.ERR_AV_FAILED,
            "av fail"
        );
    }

    private IMessage getSampleMessage(byte[] body) {
        return new Message(UUID.randomUUID().toString(), body, "content-type");
    }
}
