package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IQueueClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.QueueClientSupplier;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class ServiceBusHelperTest {

    @Mock
    IQueueClient queueClient;

    @Mock
    QueueClientSupplier queueClientSupplier;

    ServiceBusHelper serviceBusHelper;

    @Before
    public void setUp() {
        when(queueClientSupplier.get()).thenReturn(this.queueClient);
        serviceBusHelper = new ServiceBusHelper(queueClientSupplier);
    }

    @Test
    public void should_send_message_with_messageId() throws Exception {
        Msg msg = new EnvelopeMsg("envelopeId");
        serviceBusHelper.sendMessageAsync(msg);
        
        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).sendAsync(argument.capture());
        assertThat(argument.getValue())
            .extracting(IMessage::getMessageId).containsExactly(msg.getMsgId());
    }

}
