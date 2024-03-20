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
     * @return the access key
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * @return the access key name
     */
    public String getAccessKeyName() {
        return accessKeyName;
    }

    /**
     * @return the queue name
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * @return the namespace override
     */
    public Optional<String> getNamespaceOverride() {
        return namespaceOverride;
    }

    /**
     * @param accessKey the access key
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * @param accessKeyName the access key name
     */
    public void setAccessKeyName(String accessKeyName) {
        this.accessKeyName = accessKeyName;
    }

    /**
     * @param queueName the queue name
     */
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    /**
     * @param namespaceOverride the namespace override
     */
    public void setNamespaceOverride(String namespaceOverride) {
        this.namespaceOverride = Optional.ofNullable(namespaceOverride);
    }
}
