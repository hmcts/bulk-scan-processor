package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeFinaliserService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ProcessedEnvelopeNotificationHandlerTest {

    private static final String DEAD_LETTER_REASON_PROCESSING_ERROR = "Message processing error";

    @Mock
    private EnvelopeFinaliserService envelopeFinaliserService;

    @Mock
    private MessageAutoCompletor messageCompletor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProcessedEnvelopeNotificationHandler handler;

    @Before
    public void setUp() {
        handler = new ProcessedEnvelopeNotificationHandler(
            envelopeFinaliserService,
            objectMapper,
            messageCompletor
        );
    }

    @Test
    public void should_call_envelope_finaliser_when_message_is_valid() {
        // given
        UUID envelopeId = UUID.randomUUID();

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(validMessage(envelopeId));
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId);
    }

    @Test
    public void should_not_call_envelope_finaliser_when_message_is_invalid() {
        // given
        IMessage invalidMessage = new Message("invalid body");

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(invalidMessage);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verifyNoMoreInteractions(envelopeFinaliserService);
    }

    @Test
    public void should_complete_message_when_finaliser_completes_successfully() {
        // given
        UUID envelopeId = UUID.randomUUID();
        IMessage message = validMessage(envelopeId);

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId);
        verify(messageCompletor).completeAsync(message.getLockToken());
    }

    @Test
    public void should_dead_letter_message_when_envelope_not_found() {
        // given
        String exceptionMessage = "test exception";
        willThrow(new EnvelopeNotFoundException(exceptionMessage))
            .given(envelopeFinaliserService)
            .finaliseEnvelope(any());

        UUID envelopeId = UUID.randomUUID();
        IMessage message = validMessage(envelopeId);

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(message);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId);
        verify(messageCompletor).deadLetterAsync(
            message.getLockToken(),
            DEAD_LETTER_REASON_PROCESSING_ERROR,
            exceptionMessage
        );
    }

    @Test
    public void should_dead_letter_message_when_invalid() {
        // given
        IMessage invalidMessage = spy(new Message("invalid body"));
        given(invalidMessage.getLockToken()).willReturn(UUID.randomUUID());

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(invalidMessage);
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verify(messageCompletor).deadLetterAsync(
            invalidMessage.getLockToken(),
            DEAD_LETTER_REASON_PROCESSING_ERROR,
            "Failed to parse 'processed envelope' message"
        );
    }

    @Test
    public void should_not_finalise_message_when_finaliser_fails_for_unknown_reason() {
        // given
        willThrow(new RuntimeException("test exception"))
            .given(envelopeFinaliserService)
            .finaliseEnvelope(any());

        UUID envelopeId = UUID.randomUUID();

        // when
        CompletableFuture<Void> future = handler.onMessageAsync(validMessage(envelopeId));
        future.join();

        // then
        assertThat(future.isCompletedExceptionally()).isFalse();
        verifyNoMoreInteractions(messageCompletor);
    }

    private IMessage validMessage(UUID envelopeId) {
        IMessage message = spy(new Message(String.format("{\"id\":\"%s\"}", envelopeId)));
        given(message.getLockToken()).willReturn(UUID.randomUUID());
        return message;
    }
}
