package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IMessageReceiver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor.DeadLetterReason;

@RunWith(MockitoJUnitRunner.class)
public class MessageAutoCompletorTest {

    @Mock
    private IMessageReceiver receiver;

    private MessageAutoCompletor completor;

    private static final UUID LOCK_TOKEN = UUID.randomUUID();

    @Before
    public void setUp() {
        completor = new MessageAutoCompletor(receiver);
    }

    @Test
    public void should_abandon_the_message_when_properties_are_provided() {
        Map<String, Object> propertiesToUpdate = Collections.singletonMap("some key", "some value");
        completor.abandonAsync(LOCK_TOKEN, propertiesToUpdate);

        verify(receiver).abandonAsync(LOCK_TOKEN, propertiesToUpdate);
        verifyNoMoreInteractions(receiver);
    }

    @Test
    public void should_abandon_the_message_when_properties_are_not_provided() {
        completor.abandonAsync(LOCK_TOKEN);

        verify(receiver).abandonAsync(LOCK_TOKEN, Collections.emptyMap());
        verifyNoMoreInteractions(receiver);
    }

    @Test
    public void should_complete_the_message() {
        completor.completeAsync(LOCK_TOKEN);

        verify(receiver).completeAsync(LOCK_TOKEN);
        verifyNoMoreInteractions(receiver);
    }

    @Test
    public void should_dead_letter_the_message_when_reasons_are_provided() {
        DeadLetterReason reason = new DeadLetterReason("reason", "description");
        completor.deadLetterAsync(LOCK_TOKEN, reason);

        verify(receiver).deadLetterAsync(LOCK_TOKEN, reason.reason, reason.description);
        verifyNoMoreInteractions(receiver);
    }

    @Test
    public void should_dead_letter_the_message_when_reason_without_description_is_provided() {
        DeadLetterReason reason = new DeadLetterReason("reason");
        completor.deadLetterAsync(LOCK_TOKEN, reason);

        assertThat(reason.description).isNull();
        verify(receiver).deadLetterAsync(LOCK_TOKEN, reason.reason, reason.description);
        verifyNoMoreInteractions(receiver);
    }

    @Test
    public void should_dead_letter_the_message_when_reasons_are_not_provided() {
        completor.deadLetterAsync(LOCK_TOKEN);

        verify(receiver).deadLetterAsync(LOCK_TOKEN, null, null);
        verifyNoMoreInteractions(receiver);
    }
}
