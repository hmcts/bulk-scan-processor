package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for blob management.
 */
@ConfigurationProperties("storage")
public class BlobManagementProperties {

    private Integer blobCopyTimeoutInMillis;

    private Integer blobLeaseTimeout;

    private Integer blobCopyPollingDelayInMillis;

    private String blobSelectedContainer;

    private Integer blobLeaseAcquireDelayInSeconds;

    /**
     * Get blob copy timeout in milliseconds.
     * @return Blob copy timeout in milliseconds
     */
    public Integer getBlobCopyTimeoutInMillis() {
        return blobCopyTimeoutInMillis;
    }

    /**
     * Set blob copy timeout in milliseconds.
     * @param blobCopyTimeoutInMillis Blob copy timeout in milliseconds
     */
    public void setBlobCopyTimeoutInMillis(int blobCopyTimeoutInMillis) {
        this.blobCopyTimeoutInMillis = blobCopyTimeoutInMillis;
    }

    /**
     * Get blob lease timeout.
     * @return Blob lease timeout
     */
    public Integer getBlobLeaseTimeout() {
        return blobLeaseTimeout;
    }

    /**
     * Set blob lease timeout.
     * @param blobLeaseTimeout Blob lease timeout
     */
    public void setBlobLeaseTimeout(Integer blobLeaseTimeout) {
        this.blobLeaseTimeout = blobLeaseTimeout;
    }

    /**
     * Get blob copy polling delay in milliseconds.
     * @return Blob copy polling delay in milliseconds
     */
    public Integer getBlobCopyPollingDelayInMillis() {
        return blobCopyPollingDelayInMillis;
    }

    /**
     * Set blob copy polling delay in milliseconds.
     * @param blobCopyPollingDelayInMillis Blob copy polling delay in milliseconds
     */
    public void setBlobCopyPollingDelayInMillis(int blobCopyPollingDelayInMillis) {
        this.blobCopyPollingDelayInMillis = blobCopyPollingDelayInMillis;
    }

    /**
     * Get blob selected container.
     * @return Blob selected container
     */
    public String getBlobSelectedContainer() {
        return blobSelectedContainer;
    }

    /**
     * Set blob selected container.
     * @param blobSelectedContainer Blob selected container
     */
    public void setBlobSelectedContainer(String blobSelectedContainer) {
        this.blobSelectedContainer = blobSelectedContainer;
    }

    /**
     * Get blob lease acquire delay in seconds.
     * @return Blob lease acquire delay in seconds
     */
    public Integer getBlobLeaseAcquireDelayInSeconds() {
        return blobLeaseAcquireDelayInSeconds;
    }

    /**
     * Set blob lease acquire delay in seconds.
     * @param blobLeaseAcquireDelayInSeconds Blob lease acquire delay in seconds
     */
    public void setBlobLeaseAcquireDelayInSeconds(Integer blobLeaseAcquireDelayInSeconds) {
        this.blobLeaseAcquireDelayInSeconds = blobLeaseAcquireDelayInSeconds;
    }
}
