package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("accesstoken")
public class AccessTokenProperties {
    private List<TokenConfig> serviceConfig;

    public List<TokenConfig> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(List<TokenConfig> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public static class TokenConfig {
        private String serviceName;
        private int validity;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public int getValidity() {
            return validity;
        }

        public void setValidity(int validity) {
            this.validity = validity;
        }
    }
}
