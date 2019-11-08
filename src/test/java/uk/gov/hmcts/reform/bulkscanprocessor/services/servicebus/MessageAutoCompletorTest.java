package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IQueueClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class MessageAutoCompletorTest {

    @Mock
    private IQueueClient client;

    private MessageAutoCompletor completor;

    private static final UUID LOCK_TOKEN = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        completor = new MessageAutoCompletor(client);
    }

    @Test
    public void should_complete_the_message() {
        completor.completeAsync(LOCK_TOKEN);

        verify(client).completeAsync(LOCK_TOKEN);
        verifyNoMoreInteractions(client);
    }

    @Test
    public void should_dead_letter_the_message_when_reasons_are_provided() {
        completor.deadLetterAsync(LOCK_TOKEN, "reason", "description");

        verify(client).deadLetterAsync(LOCK_TOKEN, "reason", "description");
        verifyNoMoreInteractions(client);
    }
}
