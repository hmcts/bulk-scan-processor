package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.ExceptionPhase;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.msg.ProcessedEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeFinaliserService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageAutoCompletor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.MessageBodyRetriever;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler of messages form processed envelopes queue.
 * <p>
 * Its purpose is to bring envelopes referenced by those messages to their final state.
 * This involves removing sensitive information, status change and creation of an appropriate event.
 * </p>
 */
@DependsOn("processed-envelopes-completor")
@Service
@Profile(Profiles.NOT_SERVICE_BUS_STUB) // only active when interaction with Service Bus isn't disabled
public class ProcessedEnvelopeNotificationHandler implements IMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEnvelopeNotificationHandler.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final EnvelopeFinaliserService envelopeFinaliserService;
    private final ObjectMapper objectMapper;
    private final MessageAutoCompletor messageCompletor;

    public ProcessedEnvelopeNotificationHandler(
        EnvelopeFinaliserService envelopeFinaliserService,
        ObjectMapper objectMapper,
        @Qualifier("processed-envelopes-completor") MessageAutoCompletor messageCompletor
    ) {
        this.envelopeFinaliserService = envelopeFinaliserService;
        this.objectMapper = objectMapper;
        this.messageCompletor = messageCompletor;
    }

    @Override
    public CompletableFuture<Void> onMessageAsync(IMessage message) {
        return CompletableFuture
            .supplyAsync(() -> tryProcessMessage(message), EXECUTOR)
            .thenComposeAsync(processingResult -> tryFinaliseMessageAync(message, processingResult), EXECUTOR)
            .handleAsync((v, error) -> {
                // Individual steps are supposed to handle their exceptions themselves.
                // This code is here to make sure errors are logged even when they fail to do that.
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

    private CompletableFuture<Void> tryFinaliseMessageAync(
        IMessage message,
        MessageProcessingResult processingResult
    ) {
        return finaliseMessageAsync(message, processingResult)
            .exceptionally(error -> {
                log.error(
                    "An error occurred when trying to finalise 'processed envelope' message with ID {}",
                    message.getMessageId(),
                    error
                );

                return null;
            });
    }

    private CompletableFuture<Void> finaliseMessageAsync(IMessage message, MessageProcessingResult processingResult) {
        switch (processingResult.resultType) {
            case SUCCESS:
                return messageCompletor
                    .completeAsync(message.getLockToken())
                    .thenRun(() ->
                        log.info("Completed 'processed-envelope' message with ID {}", message.getMessageId())
                    );
            case UNRECOVERABLE_FAILURE:
                return messageCompletor
                    .deadLetterAsync(
                        message.getLockToken(),
                        "Message processing error",
                        processingResult.exception.getMessage()
                    )
                    .thenRun(() ->
                        log.info("Dead-lettered 'processed-envelope' message with ID {}", message.getMessageId())
                    );
            default:
                log.info(
                    "Letting 'processed envelope' message with ID {} return to the queue. Delivery attempt {}.",
                    message.getMessageId(),
                    message.getDeliveryCount() + 1
                );

                return CompletableFuture.completedFuture(null);
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

    private ProcessedEnvelope readProcessedEnvelope(IMessage message) throws IOException {
        try {
            ProcessedEnvelope processedEnvelope = objectMapper.readValue(
                MessageBodyRetriever.getBinaryData(message.getMessageBody()),
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
