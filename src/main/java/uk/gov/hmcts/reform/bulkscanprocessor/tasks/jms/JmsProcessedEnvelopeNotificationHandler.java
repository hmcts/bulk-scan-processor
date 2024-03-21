package uk.gov.hmcts.reform.bulkscanprocessor.tasks.jms;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg.ProcessedEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeFinaliserService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Handler of messages form processed envelopes queue.
 * <p>
 * Its purpose is to bring envelopes referenced by those messages to their final state.
 * This involves removing sensitive information, status change and creation of an appropriate event.
 * </p>
 */
@Service
@Profile(Profiles.NOT_SERVICE_BUS_STUB) // only active when interaction with Service Bus isn't disabled
@ConditionalOnExpression("${jms.enabled}")
public class JmsProcessedEnvelopeNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(JmsProcessedEnvelopeNotificationHandler.class);

    private final EnvelopeFinaliserService envelopeFinaliserService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for the JmsProcessedEnvelopeNotificationHandler.
     * @param envelopeFinaliserService The envelope finaliser service
     * @param objectMapper The object mapper
     */
    public JmsProcessedEnvelopeNotificationHandler(
        EnvelopeFinaliserService envelopeFinaliserService,
        ObjectMapper objectMapper
    ) {
        this.envelopeFinaliserService = envelopeFinaliserService;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes the message.
     * @param messageContext The message context
     * @param messageBody The message body
     * @throws JMSException If an error occurs
     */
    public void processMessage(Message messageContext, String messageBody) throws JMSException {
        //var message = messageContext.getMessage();
        var processingResult = tryProcessMessage(messageContext, messageBody);
        finaliseMessage(messageContext, processingResult);
    }

    /**
     * Processes the exception by logging it.
     * @param context The service bus error context
     */
    public void processException(ServiceBusErrorContext context) {
        log.error("Processed envelope queue handle error {}", context.getErrorSource(), context.getException());
    }

    /**
     * Finalises the message.
     * @param messageContext The message context
     * @param processingResult The processing result
     * @throws JMSException If an error occurs
     */
    private void finaliseMessage(
        Message messageContext,
        MessageProcessingResult processingResult
    ) throws JMSException {
        switch (processingResult.resultType) {
            case SUCCESS:
                messageContext.acknowledge();
                break;
            case UNRECOVERABLE_FAILURE:
                log.info("Processed envelope Message with ID {} has been dead-lettered. "
                             + "Error description: Message processing error. "
                             + "Reason: {}",
                         messageContext.getJMSMessageID(), processingResult.exception.getMessage()
                );
                break;
            default:
                log.info(
                    "Letting 'processed envelope' message with ID {} return to the queue. Delivery attempt {}.",
                    messageContext.getJMSMessageID(),
                    Long.parseLong(messageContext.getStringProperty("JMSXDeliveryCount")) + 1
                );
                break;
        }
    }

    /**
     * Tries to process the message.
     * @param message The message
     * @param messageBody The message body
     * @return The processing result
     * @throws JMSException If an error occurs
     */
    private MessageProcessingResult tryProcessMessage(Message message, String messageBody) throws JMSException {
        try {
            log.info(
                "Started processing 'processed envelope' message with ID {} (delivery {})",
                message.getJMSMessageID(),
                message.getStringProperty("JMSXDeliveryCount")
            );

            ProcessedEnvelope processedEnvelope = readProcessedEnvelope(messageBody);
            envelopeFinaliserService.finaliseEnvelope(
                processedEnvelope.envelopeId,
                processedEnvelope.ccdId,
                processedEnvelope.envelopeCcdAction
            );
            log.info("'Processed envelope' message with ID {} processed successfully", message.getJMSMessageID());
            return new MessageProcessingResult(MessageProcessingResultType.SUCCESS);
        } catch (InvalidMessageException e) {
            log.error("Invalid 'processed envelope' message with ID {}", message.getJMSMessageID(), e);
            return new MessageProcessingResult(MessageProcessingResultType.UNRECOVERABLE_FAILURE, e);
        } catch (EnvelopeNotFoundException e) {
            log.error(
                "Failed to handle 'processed envelope' message with ID {} - envelope not found",
                message.getJMSMessageID(),
                e
            );
            return new MessageProcessingResult(MessageProcessingResultType.UNRECOVERABLE_FAILURE, e);
        } catch (Exception e) {
            log.error(
                "An error occurred when handling 'processed envelope' message with ID {}",
                message.getJMSMessageID(),
                e
            );

            return new MessageProcessingResult(MessageProcessingResultType.POTENTIALLY_RECOVERABLE_FAILURE);
        }
    }

    /**
     * Reads the processed envelope.
     * @param messageBody The message body
     * @return The processed envelope
     * @throws IOException If an error occurs
     */
    private ProcessedEnvelope readProcessedEnvelope(String messageBody) throws IOException {
        try {
            ProcessedEnvelope processedEnvelope = objectMapper.readValue(
                messageBody.getBytes(StandardCharsets.UTF_8),
                ProcessedEnvelope.class
            );
            log.info(
                "Parsed processed envelope message, Envelope Id :{}, ccd reference :{}, Ccd Type : {}",
                processedEnvelope.envelopeId,
                processedEnvelope.ccdId,
                processedEnvelope.envelopeCcdAction
            );
            return processedEnvelope;
        } catch (JsonParseException | JsonMappingException e) {
            throw new InvalidMessageException("Failed to parse 'processed envelope' message", e);
        }
    }

    /**
     * The message processing result.
     */
    static class MessageProcessingResult {
        public final MessageProcessingResultType resultType;
        public final Exception exception;

        /**
         * Constructor for the message processing result.
         * @param resultType The result type
         */
        public MessageProcessingResult(MessageProcessingResultType resultType) {
            this(resultType, null);
        }

        /**
         * Constructor for the message processing result.
         * @param resultType The result type
         * @param exception The exception
         */
        public MessageProcessingResult(MessageProcessingResultType resultType, Exception exception) {
            this.resultType = resultType;
            this.exception = exception;
        }
    }

    /**
     * The message processing result type.
     */
    enum MessageProcessingResultType {
        SUCCESS,
        UNRECOVERABLE_FAILURE,
        POTENTIALLY_RECOVERABLE_FAILURE
    }
}
