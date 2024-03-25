package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for access token.
 */
@ConfigurationProperties("accesstoken")
public class AccessTokenProperties {
    private List<TokenConfig> serviceConfig;

    /**
     * Get configuration properties for access token.
     * @return List of TokenConfig
     */
    public List<TokenConfig> getServiceConfig() {
        return serviceConfig;
    }

    /**
     * Set configuration properties for access token.
     * @param serviceConfig List of TokenConfig
     */
    public void setServiceConfig(List<TokenConfig> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    /**
     * Configuration properties for access token.
     */
    public static class TokenConfig {
        private String serviceName;
        private int validity;

        /**
         * Get service name.
         * @return Service name
         */
        public String getServiceName() {
            return serviceName;
        }

        /**
         * Set service name.
         * @param serviceName Service name
         */
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * Get validity.
         * @return Validity
         */
        public int getValidity() {
            return validity;
        }

        /**
         * Set validity.
         * @param validity Validity
         */
        public void setValidity(int validity) {
            this.validity = validity;
        }
    }
}
