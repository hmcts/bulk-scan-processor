package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "envelope-access")
public class EnvelopeAccessProperties {

    private List<Mapping> mappings;

    // region getters and setters
    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }
    // endregion

    public static class Mapping {

        private String jurisdiction;
        private String readService;
        private String updateService;

        // region constructor, getters and setters
        public Mapping(String jurisdiction, String readService, String updateService) {
            this.jurisdiction = jurisdiction;
            this.readService = readService;
            this.updateService = updateService;
        }

        public Mapping() {
            // Spring needs it.
        }

        public String getJurisdiction() {
            return jurisdiction;
        }

        public void setJurisdiction(String jurisdiction) {
            this.jurisdiction = jurisdiction;
        }

        public String getReadService() {
            return readService;
        }

        public void setReadService(String readService) {
            this.readService = readService;
        }

        public String getUpdateService() {
            return updateService;
        }

        public void setUpdateService(String updateService) {
            this.updateService = updateService;
        }
        // endregion
    }
}
