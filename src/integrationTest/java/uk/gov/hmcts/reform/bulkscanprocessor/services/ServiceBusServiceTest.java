package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.QueueClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.EnvelopeMsg;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class ServiceBusServiceTest {

    @Mock
    QueueClient queueClient;

    @Autowired
    ServiceBusService serviceBusService;

    @Test
    public void should_send_message_with_messageId() throws Exception {
        Msg msg = new EnvelopeMsg("envelopeId");
        serviceBusService.sendMessageAsync(msg);

        ArgumentCaptor<IMessage> argument = ArgumentCaptor.forClass(IMessage.class);
        verify(queueClient).sendAsync(argument.capture());
        assertThat(argument.getValue()).extracting(IMessage::getMessageId).isEqualTo(msg.getMsgId());
    }

}
