package uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConnectionException;

import java.util.function.Supplier;

@Component
public class QueueClientSupplier implements Supplier<IQueueClient> {

    private final String connectionString;

    public QueueClientSupplier(@Value("${servicebus.queue_envelope_send}") String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public IQueueClient get() {
        try {
            return new QueueClient(
                new ConnectionStringBuilder(connectionString),
                ReceiveMode.PEEKLOCK
            );
        } catch (InterruptedException | ServiceBusException exception) {
            throw new ConnectionException("Unable to connect to Azure service bus", exception);
        }
    }
}
