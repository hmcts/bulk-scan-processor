package uk.gov.hmcts.reform.bulkscanprocessor.config;

import java.util.Optional;

/**
 * Configuration properties for the queue.
 */
public class QueueConfigurationProperties {

    private String accessKey;
    private String accessKeyName;
    private String queueName;
    private Optional<String> namespaceOverride = Optional.empty();

    /**
     * Get the access key.
     * @return the access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Get the access key name.
     * @return the access key name
     */
    public String getAccessKeyName() {
        return accessKeyName;
    }

    /**
     * Get the queue name.
     * @return the queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * Get the namespace override.
     * @return the namespace override
     */
    public Optional<String> getNamespaceOverride() {
        return namespaceOverride;
    }

    /**
     * Set the access key.
     * @param accessKey the access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Set the access key name.
     * @param accessKeyName the access key name
     */
    public void setAccessKeyName(String accessKeyName) {
        this.accessKeyName = accessKeyName;
    }

    /**
     * Set the queue name.
     * @param queueName the queue name
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * Set the namespace override.
     * @param namespaceOverride the namespace override
     */
    public void setNamespaceOverride(String namespaceOverride) {
        this.namespaceOverride = Optional.ofNullable(namespaceOverride);
    }
}
