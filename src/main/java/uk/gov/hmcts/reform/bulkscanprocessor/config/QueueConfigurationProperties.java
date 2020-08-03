package uk.gov.hmcts.reform.bulkscanprocessor.config;

import java.util.Optional;

public class QueueConfigurationProperties {

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
