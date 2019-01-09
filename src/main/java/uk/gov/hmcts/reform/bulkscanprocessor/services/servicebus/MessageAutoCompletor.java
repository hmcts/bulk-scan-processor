package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IMessageReceiver;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MessageAutoCompletor {

    public static class DeadLetterReason {

        String reason;

        String description;

        public DeadLetterReason(String reason, String description) {
            this.reason = reason;
            this.description = description;
        }

        public DeadLetterReason(String reason) {
            this(reason, null);
        }
    }

    private final BiFunction<UUID, Map<String, Object>, CompletableFuture<Void>> abandonAsyncDelegate;

    private final Function<UUID, CompletableFuture<Void>> completeAsyncDelegate;

    private final BiFunction<UUID, DeadLetterReason, CompletableFuture<Void>> deadLetterAsyncDelegate;

    public MessageAutoCompletor(IMessageReceiver receiver) {
        abandonAsyncDelegate = receiver::abandonAsync;
        completeAsyncDelegate = receiver::completeAsync;
        deadLetterAsyncDelegate = (uuid, deadLetterReason) -> receiver.deadLetterAsync(
            uuid,
            deadLetterReason.reason,
            deadLetterReason.description
        );
    }

    public CompletableFuture<Void> abandonAsync(UUID lockToken, Map<String, Object> propertiesToUpdate) {
        return abandonAsyncDelegate.apply(lockToken, propertiesToUpdate);
    }

    public CompletableFuture<Void> abandonAsync(UUID lockToken) {
        return abandonAsync(lockToken, Collections.emptyMap());
    }

    public CompletableFuture<Void> completeAsync(UUID lockToken) {
        return completeAsyncDelegate.apply(lockToken);
    }

    public CompletableFuture<Void> deadLetterAsync(UUID lockToken, DeadLetterReason deadLetterReason) {
        return deadLetterAsyncDelegate.apply(lockToken, deadLetterReason);
    }

    public CompletableFuture<Void> deadLetterAsync(UUID lockToken) {
        return deadLetterAsync(lockToken, new DeadLetterReason(null, null));
    }
}
