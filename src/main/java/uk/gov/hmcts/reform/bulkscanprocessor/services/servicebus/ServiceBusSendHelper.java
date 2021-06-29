package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import javax.annotation.PreDestroy;

public class ServiceBusSendHelper implements AutoCloseable {

    private final ServiceBusSenderClient sendClient;

    private final ObjectMapper objectMapper;

    public ServiceBusSendHelper(ServiceBusSenderClient sendClient, ObjectMapper objectMapper) {
        this.sendClient = sendClient;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(Msg msg) {
        ServiceBusMessage busMessage = mapToBusMessage(msg);
        try {
            sendClient.sendMessage(busMessage);
        } catch (ServiceBusException e) {
            throw new InvalidMessageException("Unable to send message", e);
        }
    }

    @Override
    public void close() {
        if (sendClient != null) {
            sendClient.close();
        }
    }

    ServiceBusMessage mapToBusMessage(Msg msg) {
        if (msg == null) {
            throw new InvalidMessageException("Msg == null");
        }
        if (Strings.isNullOrEmpty(msg.getMsgId())) {
            throw new InvalidMessageException("Msg Id == null");
        }
        var serviceBusMessage = new ServiceBusMessage(BinaryData.fromString(getMsgBodyInString(msg)));
        serviceBusMessage.setContentType("application/json");
        serviceBusMessage.setMessageId(msg.getMsgId());
        serviceBusMessage.setSubject(msg.getLabel());

        return serviceBusMessage;
    }

    private String getMsgBodyInString(Msg message) {
        try {
            return objectMapper.writeValueAsString(message); //default encoding is UTF-8
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        close();
    }

}

