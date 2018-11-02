package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.concurrent.CompletableFuture;
import javax.annotation.PreDestroy;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.MsgLabel.TEST;

@Component
@Lazy
public class ServiceBusHelper implements AutoCloseable {

    private final IQueueClient sendClient;

    private final ObjectMapper objectMapper;

    ServiceBusHelper(IQueueClient queueClient, ObjectMapper objectMapper) {
        this.sendClient = queueClient;
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

    @Override
    public void close() {
        if (sendClient != null) {
            sendClient.closeAsync();
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
        if (msg.isTestOnly()) {
            busMessage.setLabel(TEST.toString());
        }
        return busMessage;
    }

    private byte[] getMsgBodyInBytes(Msg message) {
        try {
            return objectMapper.writeValueAsBytes(message); //default encoding is UTF-8
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

}

