package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.ErrorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.spy;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
public class ErrorNotificationHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final UUID LOCK_TOKEN = UUID.randomUUID();

    private static final CompletableFuture<Void> COMPLETED_FUTURE = CompletableFuture.completedFuture(null);

    @Mock
    private ErrorNotificationService service;

    @Mock
    private IQueueClient queueClient;

    private ErrorNotificationHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new ErrorNotificationHandler(service, MAPPER, new MessageAutoCompletor(queueClient));
    }

    @Test
    public void should_handle_invalid_message_when_trying_parse_the_body() {
        // given
        IMessage message = spy(getSampleMessage("{}".getBytes()));
        given(message.getLockToken()).willReturn(LOCK_TOKEN);
        given(queueClient.deadLetterAsync(eq(LOCK_TOKEN), anyString(), anyString())).willReturn(COMPLETED_FUTURE);

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        verify(service, never()).processServiceBusMessage(any(ErrorMsg.class));
    }

    @Test
    public void should_handle_error_when_service_throws_one() throws JsonProcessingException {
        // given
        ErrorMsg msg = getSampleErrorMessage();
        IMessage message = spy(getSampleMessage(MAPPER.writeValueAsBytes(msg)));
        willThrow(new RuntimeException("oh no")).given(service).processServiceBusMessage(any(ErrorMsg.class));
        given(message.getLockToken()).willReturn(LOCK_TOKEN);
        given(queueClient.deadLetterAsync(eq(LOCK_TOKEN), anyString(), anyString())).willReturn(COMPLETED_FUTURE);

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();

        // and
        verify(service).processServiceBusMessage(any(ErrorMsg.class));
    }

    @Test
    public void should_complete_task_successfully() throws JsonProcessingException {
        // given
        ErrorMsg msg = getSampleErrorMessage();
        IMessage message = spy(getSampleMessage(MAPPER.writeValueAsBytes(msg)));
        doNothing().when(service).processServiceBusMessage(any(ErrorMsg.class));
        given(message.getLockToken()).willReturn(LOCK_TOKEN);
        given(queueClient.completeAsync(LOCK_TOKEN)).willReturn(COMPLETED_FUTURE);

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
            "av fail",
            "service1",
            "container1"
        );
    }

    private IMessage getSampleMessage(byte[] body) {
        return new Message(UUID.randomUUID().toString(), body, "content-type");
    }
}
