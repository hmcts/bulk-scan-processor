package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;

@Service
public class ServiceBusService {

    private final QueueClient sendClient;

    public ServiceBusService(QueueClient sendClient) {
        this.sendClient = sendClient;
    }

    public CompletableFuture<Void> sendMessageAsync(Msg msg) {
        Message busMessage = new Message();
        busMessage.setMessageId(msg.getMsgId());
        return sendClient.sendAsync(busMessage);
    }

}
