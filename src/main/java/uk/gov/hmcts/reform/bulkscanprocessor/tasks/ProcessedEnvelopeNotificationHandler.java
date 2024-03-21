package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
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

/**
 * Handler of messages form processed envelopes queue.
 * <p>
 * Its purpose is to bring envelopes referenced by those messages to their final state.
 * This involves removing sensitive information, status change and creation of an appropriate event.
 * </p>
 */
@Service
@Profile(Profiles.NOT_SERVICE_BUS_STUB) // only active when interaction with Service Bus isn't disabled
@ConditionalOnExpression("!${jms.enabled}")
public class ProcessedEnvelopeNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEnvelopeNotificationHandler.class);

    private final EnvelopeFinaliserService envelopeFinaliserService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor for the ProcessedEnvelopeNotificationHandler.
     * @param envelopeFinaliserService The envelope finaliser service
     * @param objectMapper The object mapper
     */
    public ProcessedEnvelopeNotificationHandler(
        EnvelopeFinaliserService envelopeFinaliserService,
        ObjectMapper objectMapper
    ) {
        this.envelopeFinaliserService = envelopeFinaliserService;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes the message from the queue.
     * @param messageContext The message context
     */
    public void processMessage(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();
        var processingResult = tryProcessMessage(message);
        finaliseMessage(messageContext, processingResult);
    }

    /**
     * Processes the exception.
     * @param context The error context
     */
    public void processException(ServiceBusErrorContext context) {
        log.error("Processed envelope queue handle error {}", context.getErrorSource(), context.getException());
    }

    /**
     * Finalises the message.
     * @param messageContext The message context
     * @param processingResult The processing result
     */
    private void finaliseMessage(
        ServiceBusReceivedMessageContext messageContext,
        MessageProcessingResult processingResult
    ) {
        var message = messageContext.getMessage();
        switch (processingResult.resultType) {
            case SUCCESS:
                messageContext.complete();
                break;
            case UNRECOVERABLE_FAILURE:
                messageContext.deadLetter(
                    new DeadLetterOptions()
                        .setDeadLetterErrorDescription("Message processing error")
                        .setDeadLetterReason(processingResult.exception.getMessage())
                );
                break;
            default:
                log.info(
                    "Letting 'processed envelope' message with ID {} return to the queue. Delivery attempt {}.",
                    message.getMessageId(),
                    message.getDeliveryCount() + 1
                );
                break;
        }
    }

    /**
     * Tries to process the message.
     * @param message The message
     * @return The processing result
     * @throws MessageProcessingResult The message processing result
     */
    private MessageProcessingResult tryProcessMessage(ServiceBusReceivedMessage message) {
        try {
            log.info(
                "Started processing 'processed envelope' message with ID {} (delivery {})",
                message.getMessageId(),
                message.getDeliveryCount() + 1
            );

            ProcessedEnvelope processedEnvelope = readProcessedEnvelope(message);
            envelopeFinaliserService.finaliseEnvelope(
                processedEnvelope.envelopeId,
                processedEnvelope.ccdId,
                processedEnvelope.envelopeCcdAction
            );
            log.info("'Processed envelope' message with ID {} processed successfully", message.getMessageId());
            return new MessageProcessingResult(MessageProcessingResultType.SUCCESS);
        } catch (InvalidMessageException e) {
            log.error("Invalid 'processed envelope' message with ID {}", message.getMessageId(), e);
            return new MessageProcessingResult(MessageProcessingResultType.UNRECOVERABLE_FAILURE, e);
        } catch (EnvelopeNotFoundException e) {
            log.error(
                "Failed to handle 'processed envelope' message with ID {} - envelope not found",
                message.getMessageId(),
                e
            );
            return new MessageProcessingResult(MessageProcessingResultType.UNRECOVERABLE_FAILURE, e);
        } catch (Exception e) {
            log.error(
                "An error occurred when handling 'processed envelope' message with ID {}",
                message.getMessageId(),
                e
            );

            return new MessageProcessingResult(MessageProcessingResultType.POTENTIALLY_RECOVERABLE_FAILURE);
        }
    }

    /**
     * Reads the processed envelope.
     * @param message The message
     * @return The processed envelope
     * @throws IOException The IO exception
     */
    private ProcessedEnvelope readProcessedEnvelope(ServiceBusReceivedMessage message) throws IOException {
        try {
            ProcessedEnvelope processedEnvelope = objectMapper.readValue(
                message.getBody().toBytes(),
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
         * Constructor for the MessageProcessingResult.
         * @param resultType The result type
         */
        public MessageProcessingResult(MessageProcessingResultType resultType) {
            this(resultType, null);
        }

        /**
         * Constructor for the MessageProcessingResult.
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
