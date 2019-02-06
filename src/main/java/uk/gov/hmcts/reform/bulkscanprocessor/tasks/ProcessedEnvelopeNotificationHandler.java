package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg.ProcessedEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeFinaliserService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Handler of messages form processed envelopes queue.
 *
 * <p>
 *   Its purpose is to bring envelopes referenced by those messages to their final state.
 *   This involves removing sensitive information, status change and creation of an appropriate event.
 * </p>
 */
public class ProcessedEnvelopeNotificationHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEnvelopeNotificationHandler.class);

    private final EnvelopeFinaliserService envelopeFinaliserService;
    private final ObjectMapper objectMapper;
    private final MessageAutoCompletor messageCompletor;

    public ProcessedEnvelopeNotificationHandler(
        EnvelopeFinaliserService envelopeFinaliserService,
        ObjectMapper objectMapper,
        MessageAutoCompletor messageCompletor
    ) {
        this.envelopeFinaliserService = envelopeFinaliserService;
        this.objectMapper = objectMapper;
        this.messageCompletor = messageCompletor;
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage message) {
        return CompletableFuture
            .supplyAsync(() -> tryProcessMessage(message))
            .thenAcceptAsync(processingResult -> tryFinaliseMessage(message, processingResult))
            .handleAsync((v, error) -> {
                if (error != null) {
                    log.error(
                        "An error occurred when trying to handle 'processed envelope' message with ID {}",
                        message.getMessageId()
                    );
                }

                return null;
            });
    }

    @Override
    public void notifyException(Throwable throwable, ExceptionPhase exceptionPhase) {
        log.error(
            "An error occurred when handling processed envelope notification. Phase: {}",
            exceptionPhase,
            throwable
        );
    }

    private void tryFinaliseMessage(IMessage message, MessageProcessingResult processingResult) {
        try {
            finaliseMessage(message, processingResult);
        } catch (Exception e) {
            log.error(
                "An error occurred when trying to finalise 'processed envelope' message with ID {}",
                message.getMessageId(),
                e
            );
        }
    }

    private void finaliseMessage(IMessage message, MessageProcessingResult processingResult) {
        switch (processingResult.resultType) {
            case SUCCESS:
                messageCompletor.completeAsync(message.getLockToken()).join();
                log.info("Completed 'processed-envelope' message with ID {}", message.getMessageId());
                break;
            case UNRECOVERABLE_FAILURE:
                messageCompletor.deadLetterAsync(
                    message.getLockToken(),
                    "Message processing error",
                    processingResult.exception.getMessage()
                ).join();

                log.info("Dead-lettered 'processed-envelope' message with ID {}", message.getMessageId());
                break;
            default:
                log.info(
                    "Letting 'processed envelope' message with ID {} return to the queue. Delivery attempt {}.",
                    message.getMessageId(),
                    message.getDeliveryCount() + 1
                );
        }
    }

    private MessageProcessingResult tryProcessMessage(IMessage message) {
        try {
            log.info(
                "Started processing 'processed envelope' message with ID {} (delivery {})",
                message.getMessageId(),
                message.getDeliveryCount() + 1
            );

            ProcessedEnvelope processedEnvelope = readProcessedEnvelope(message);
            envelopeFinaliserService.finaliseEnvelope(processedEnvelope.id);
            log.info("'Processed envelope' message with ID {} processed successfully", message.getMessageId());
            return new MessageProcessingResult(MessageProcessingResultType.SUCCESS);
        } catch (InvalidMessageException e) {
            log.error("Invalid 'processed envelope' message with ID {}", message.getMessageId(), e);
            return new MessageProcessingResult(MessageProcessingResultType.UNRECOVERABLE_FAILURE, e);
        } catch (EnvelopeNotFoundException e) {
            log.error("Failed to handle 'processed envelope' message with ID {} - envelope not found", e);
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

    private ProcessedEnvelope readProcessedEnvelope(IMessage message) throws IOException {
        try {
            return objectMapper.readValue(message.getBody(), ProcessedEnvelope.class);
        } catch (JsonParseException | JsonMappingException e) {
            throw new InvalidMessageException("Failed to parse 'processed envelope' message", e);
        }
    }

    class MessageProcessingResult {
        public final MessageProcessingResultType resultType;
        public final Exception exception;

        public MessageProcessingResult(MessageProcessingResultType resultType) {
            this(resultType, null);
        }

        public MessageProcessingResult(MessageProcessingResultType resultType, Exception exception) {
            this.resultType = resultType;
            this.exception = exception;
        }
    }

    enum MessageProcessingResultType {
        SUCCESS,
        UNRECOVERABLE_FAILURE,
        POTENTIALLY_RECOVERABLE_FAILURE
    }
}
