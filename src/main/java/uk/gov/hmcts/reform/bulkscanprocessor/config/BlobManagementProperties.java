package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("storage")
public class BlobManagementProperties {

    private Integer blobCopyTimeoutInMillis;

    private Integer blobLeaseTimeout;

    private Integer blobCopyPollingDelayInMillis;

    private String blobSelectedContainer;

    public Integer getBlobCopyTimeoutInMillis() {
        return blobCopyTimeoutInMillis;
    }

    public void setBlobCopyTimeoutInMillis(int blobCopyTimeoutInMillis) {
        this.blobCopyTimeoutInMillis = blobCopyTimeoutInMillis;
    }

    public Integer getBlobLeaseTimeout() {
        return blobLeaseTimeout;
    }

    public void setBlobLeaseTimeout(Integer blobLeaseTimeout) {
        this.blobLeaseTimeout = blobLeaseTimeout;
    }

    public Integer getBlobCopyPollingDelayInMillis() {
        return blobCopyPollingDelayInMillis;
    }

    public void setBlobCopyPollingDelayInMillis(int blobCopyPollingDelayInMillis) {
        this.blobCopyPollingDelayInMillis = blobCopyPollingDelayInMillis;
    }

    public String getBlobSelectedContainer() {
        return blobSelectedContainer;
    }

    public void setBlobSelectedContainer(String blobSelectedContainer) {
        this.blobSelectedContainer = blobSelectedContainer;
    }
}
