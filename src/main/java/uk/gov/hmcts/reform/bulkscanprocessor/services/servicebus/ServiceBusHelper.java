package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;

@Component
@Lazy
public class ServiceBusHelper {

    private final IQueueClient sendClient;

    public ServiceBusHelper(QueueClientSupplier queueClientSupplier) {
        this.sendClient = queueClientSupplier.get();
    }

    public CompletableFuture<Void> sendMessageAsync(Msg msg) {
        Message busMessage = new Message();
        busMessage.setMessageId(msg.getMsgId());
        return sendClient.sendAsync(busMessage);
    }

}
