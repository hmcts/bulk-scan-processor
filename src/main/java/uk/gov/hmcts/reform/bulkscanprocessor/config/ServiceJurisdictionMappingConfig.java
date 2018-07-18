package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "mapping")
public class ServiceJurisdictionMappingConfig {
    private Map<String, String> servicesJurisdiction = new HashMap<>();

    public Map<String, String> getServicesJurisdiction() {
        return servicesJurisdiction;
    }

    public void setServicesJurisdiction(Map<String, String> servicesJurisdiction) {
        this.servicesJurisdiction = servicesJurisdiction;
    }
}
