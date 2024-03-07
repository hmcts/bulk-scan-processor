package uk.gov.hmcts.reform.bulkscanprocessor.services.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.Msg;

import java.util.Objects;
import javax.jms.JMSException;
import javax.jms.Message;

public class JmsQueueSendHelper {

    private JmsTemplate jmsTemplate;

    private final ObjectMapper objectMapper;

    public JmsQueueSendHelper(JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(Msg msg) {
        String jmsMessage = mapToJmsMessage(msg);

        jmsTemplate.convertAndSend(
            Objects.requireNonNull(jmsTemplate.getDefaultDestinationName()),
            jmsMessage,
            new MessagePostProcessor() {
                @Override
                public jakarta.jms.Message postProcessMessage(jakarta.jms.Message message) throws jakarta.jms.JMSException {
                    message.setJMSMessageID(msg.getMsgId());
                    message.setStringProperty("contentType", "application/json");
                    message.setStringProperty("subject", msg.getLabel());
                    return message;
                }

            }
        );
    }

    String mapToJmsMessage(Msg msg) {
        if (msg == null) {
            throw new InvalidMessageException("Msg == null");
        }
        if (Strings.isNullOrEmpty(msg.getMsgId())) {
            throw new InvalidMessageException("Msg Id == null");
        }

        try {
            return objectMapper.writeValueAsString(msg); //default encoding is UTF-8
        } catch (JsonProcessingException e) {
            throw new InvalidMessageException("Unable to create message body in json format", e);
        }
    }
}

