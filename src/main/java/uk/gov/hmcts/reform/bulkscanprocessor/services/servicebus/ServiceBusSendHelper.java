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

/**
 * Helper class to send messages to Service Bus.
 */
public class ServiceBusSendHelper {

    private final ServiceBusSenderClient sendClient;

    private final ObjectMapper objectMapper;

    /**
     * Constructor for the ServiceBusSendHelper.
     * @param sendClient The client to send messages to the service bus
     * @param objectMapper The object mapper to convert messages to json
     */
    public ServiceBusSendHelper(ServiceBusSenderClient sendClient, ObjectMapper objectMapper) {
        this.sendClient = sendClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends a message to the service bus.
     * @param msg The message to send
     * @throws InvalidMessageException If the message is invalid
     */
    public void sendMessage(Msg msg) {
        ServiceBusMessage busMessage = mapToBusMessage(msg);
        try {
            sendClient.sendMessage(busMessage);
        } catch (ServiceBusException e) {
            throw new InvalidMessageException("Unable to send message", e);
        }
    }

    /**
     * Maps a message to a ServiceBusMessage.
     * @param msg The message to map
     * @return The ServiceBusMessage
     * @throws InvalidMessageException If the message is invalid
     */
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

    /**
     * Converts a message to a string.
     * @param message The message to convert
     * @return The message as a string
     * @throws InvalidMessageException If the message is invalid
     */
    private String getMsgBodyInString(Msg message) {
        try {
            return objectMapper.writeValueAsString(message); //default encoding is UTF-8
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }
}

