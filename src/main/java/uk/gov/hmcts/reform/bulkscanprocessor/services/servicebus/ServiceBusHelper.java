package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;

@Component
// Service bus clients are expensive to create and thread safe so perfectly reusable.
// With prototype scope we can have more than 1 client instance. Also injection happens
// when the supplier is accessed for the first time (not at application startup), this
// allows to avoid mocking the supplier in every single integration test.
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, // every other call to return bean will create new instance
    proxyMode = ScopedProxyMode.TARGET_CLASS // allows prototyping
)
public class ServiceBusHelper {

    private final IQueueClient sendClient;

    private final ObjectMapper objectMapper;

    ServiceBusHelper(QueueClientSupplier queueClientSupplier, ObjectMapper objectMapper) {
        this.sendClient = queueClientSupplier.get();
        this.objectMapper = objectMapper;
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

    Message mapToBusMessage(Msg msg) {
        if (msg == null) {
            throw new InvalidMessageException("Msg == null");
        }
        if (Strings.isNullOrEmpty(msg.getMsgId())) {
            throw new InvalidMessageException("Msg Id == null");
        }
        Message busMessage = new Message();
        busMessage.setContentType("application/json");
        busMessage.setMessageId(msg.getMsgId());
        busMessage.setBody(getMsgBodyInBytes(msg));
        return busMessage;
    }

    private byte[] getMsgBodyInBytes(Msg message) {
        try {
            String bytes = objectMapper.writeValueAsString(message); //default encoding is UTF-8
//            objectMapper.writeValueAsBytes(message);
            System.out.println(bytes);
            return bytes.getBytes();
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }
}

