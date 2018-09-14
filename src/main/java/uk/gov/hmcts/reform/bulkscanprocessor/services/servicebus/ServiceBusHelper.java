package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.google.common.base.Strings;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;

@Component
// Service bus clients are expensive to create and thread safe so perfectly reusable.
// With prototype scope we can have more than 1 client instance. Also injection happens
// when the supplier is accessed for the first time (not at application startup), this
// allows to avoid mocking the supplier in every single integration test.
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ServiceBusHelper {

    private final IQueueClient sendClient;

    public ServiceBusHelper(QueueClientSupplier queueClientSupplier) {
        this.sendClient = queueClientSupplier.get();
    }

    public CompletableFuture<Void> sendMessageAsync(Msg msg) {
        Message busMessage = mapToBusMessage(msg);
        return sendClient.sendAsync(busMessage);
    }

    public void sendMessage(Msg msg) {
        Message busMessage = mapToBusMessage(msg);
        try {
            sendClient.send(busMessage);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidMessageException("Unable to send message", e);
        } catch (ServiceBusException e) {
            throw new InvalidMessageException("Unable to send message", e);
        }
    }

    private Message mapToBusMessage(Msg msg) {
        if (msg == null) {
            throw new InvalidMessageException("Msg == null");
        }
        if (Strings.isNullOrEmpty(msg.getMsgId())) {
            throw new InvalidMessageException("Msg Id == null");
        }
        Message busMessage = new Message();
        busMessage.setMessageId(msg.getMsgId());
        busMessage.setBody(msg.getMsgBody());
        return busMessage;
    }

}
