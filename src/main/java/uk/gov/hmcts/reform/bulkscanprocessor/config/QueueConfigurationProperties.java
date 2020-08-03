package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "queues")
public class QueueConfigurationProperties {

    private String defaultNamespace;

    // bean prefix as a key
    private Map<String, QueueConfigurationItem> queues;

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public Map<String, QueueConfigurationItem> getQueues() {
        return queues;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public void setQueues(Map<String, QueueConfigurationItem> queues) {
        this.queues = queues;
    }

    static class QueueConfigurationItem {

        private String accessKey;
        private String accessKeyName;
        private String connectionString;
        private String queueName;
        private Optional<String> namespaceOverride = Optional.empty();

        public String getAccessKey() {
            return accessKey;
        }

        public String getAccessKeyName() {
            return accessKeyName;
        }

        public String getConnectionString() {
            return connectionString;
        }

        public String getQueueName() {
            return queueName;
        }

        public Optional<String> getNamespaceOverride() {
            return namespaceOverride;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public void setAccessKeyName(String accessKeyName) {
            this.accessKeyName = accessKeyName;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public void setNamespaceOverride(String namespaceOverride) {
            this.namespaceOverride = Optional.ofNullable(namespaceOverride);
        }
    }
}
