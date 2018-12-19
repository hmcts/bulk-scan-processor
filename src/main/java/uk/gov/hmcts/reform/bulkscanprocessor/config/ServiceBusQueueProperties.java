package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "queues")
class ServiceBusQueueProperties {

    private Map<ServiceBusQueues, Queue> mappings;

    public Map<ServiceBusQueues, Queue> getMappings() {
        return mappings;
    }

    public void setMappings(List<Queue> queues) {
        this.mappings = new EnumMap<>(queues
            .stream()
            .collect(Collectors.toMap(Queue::getQueue, Function.identity()))
        );
    }

    public static class Queue {

        private ServiceBusQueueTypes type;

        private ServiceBusQueues queueName;

        private String connectionString;

        public Queue() {
            // configuration section constructor
        }

        public ServiceBusQueueTypes getType() {
            return type;
        }

        public void setType(String type) {
            this.type = ServiceBusQueueTypes.valueOf(type.toUpperCase());
        }

        public ServiceBusQueues getQueue() {
            return queueName;
        }

        public String getQueueName() {
            return queueName.getQueueName();
        }

        public void setQueueName(String queueName) {
            this.queueName = ServiceBusQueues.fromQueueName(queueName);
        }

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }
    }
}
