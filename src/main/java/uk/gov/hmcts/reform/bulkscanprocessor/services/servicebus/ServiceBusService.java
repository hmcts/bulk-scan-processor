package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;

@Service
public class ServiceBusService {

    private final IQueueClient sendClient;

    public ServiceBusService(QueueClientSupplier queueClientSupplier) {
        this.sendClient = queueClientSupplier.get();
    }

    public CompletableFuture<Void> sendMessageAsync(Msg msg) {
        Message busMessage = new Message();
        busMessage.setMessageId(msg.getMsgId());
        return sendClient.sendAsync(busMessage);
    }

}
