package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeFinaliserService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ProcessedEnvelopeNotificationHandlerTest {

    private static final String DEAD_LETTER_REASON_PROCESSING_ERROR = "Message processing error";

    @Mock
    private EnvelopeFinaliserService envelopeFinaliserService;
    @Mock
    private ServiceBusReceivedMessageContext messageContext = mock(ServiceBusReceivedMessageContext.class);
    @Mock
    private ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ProcessedEnvelopeNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProcessedEnvelopeNotificationHandler(
            envelopeFinaliserService,
            objectMapper
        );
    }

    @Test
    void should_call_envelope_finaliser_when_message_is_valid() {
        // given
        UUID envelopeId = UUID.randomUUID();
        String ccdId = "123123";
        String envelopeCcdAction = "AUTO_ATTACHED_TO_CASE";
        String bodyStr = validMessage(envelopeId, ccdId, envelopeCcdAction);

        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody()).willReturn(BinaryData.fromString(bodyStr));
        // when
        handler.processMessage(messageContext);

        // then
        verify(messageContext, times(2)).getMessage();
        verify(messageContext).complete();
        verifyNoMoreInteractions(messageContext);
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId, ccdId, envelopeCcdAction);
    }

    @Test
    void should_not_call_envelope_finaliser_when_message_is_invalid() {
        // given
        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody()).willReturn(BinaryData.fromString("invalid body"));
        // when
        handler.processMessage(messageContext);

        // then
        verifyNoMoreInteractions(envelopeFinaliserService);
    }

    @Test
    void should_complete_message_when_finaliser_completes_successfully() {
        // given
        UUID envelopeId = UUID.randomUUID();
        String ccdId = "312312";
        String envelopeCcdAction = "EXCEPTION_RECORD";
        String bodyStr = validMessage(envelopeId, ccdId, envelopeCcdAction);
        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody()).willReturn(BinaryData.fromString(bodyStr));

        // when
        handler.processMessage(messageContext);

        // then
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId, ccdId, envelopeCcdAction);
        verify(messageContext, times(2)).getMessage();
        verify(messageContext).complete();
        verifyNoMoreInteractions(messageContext);
    }

    @Test
    void should_dead_letter_message_when_envelope_not_found() {
        // given
        String exceptionMessage = "test exception";
        willThrow(new EnvelopeNotFoundException(exceptionMessage))
            .given(envelopeFinaliserService)
            .finaliseEnvelope(any(), any(), any());

        UUID envelopeId = UUID.randomUUID();
        String bodyStr = validMessage(envelopeId, null, null);
        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody()).willReturn(BinaryData.fromString(bodyStr));

        // when
        handler.processMessage(messageContext);

        // then
        verify(envelopeFinaliserService).finaliseEnvelope(envelopeId, null, null);
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageContext).deadLetter(
            deadLetterOptionsArgumentCaptor.capture()
        );
        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo(exceptionMessage);
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo(DEAD_LETTER_REASON_PROCESSING_ERROR);
    }

    @Test
    void should_dead_letter_message_when_invalid() {
        // given
        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody()).willReturn(BinaryData.fromString("invalid body"));

        // when
        handler.processMessage(messageContext);

        // then
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageContext).deadLetter(
            deadLetterOptionsArgumentCaptor.capture()
        );
        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Failed to parse 'processed envelope' message");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo(DEAD_LETTER_REASON_PROCESSING_ERROR);
    }

    @Test
    void should_not_finalise_message_when_finaliser_fails_for_unknown_reason() {
        // given
        willThrow(new RuntimeException("test exception"))
            .given(envelopeFinaliserService)
            .finaliseEnvelope(any(), any(), any());

        given(messageContext.getMessage()).willReturn(message);
        given(message.getBody())
            .willReturn(BinaryData.fromString(validMessage(UUID.randomUUID(), null, null)));
        // when
        handler.processMessage(messageContext);

        // then
        verify(messageContext, times(2)).getMessage();
        verifyNoMoreInteractions(messageContext);
    }

    //ProcessedEnvelope should ignore unknown fields when json deserialization
    private String validMessage(UUID envelopeId, String ccdId, String envelopeCcdAction) {
        return
            String.format(
                " {\"envelope_id\":\"%1$s\",\"ccd_id\":%2$s,\"envelope_ccd_action\":%3$s,\"dummy\":\"xx\"}",
                envelopeId,
                ccdId == null ? null : ("\"" + ccdId + "\""),
                envelopeCcdAction == null ? null : ("\"" + envelopeCcdAction + "\"")
        );
    }

}
