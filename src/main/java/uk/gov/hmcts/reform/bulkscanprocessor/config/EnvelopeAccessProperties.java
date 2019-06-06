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

        // region constructor, getters and setters
        public Mapping(String jurisdiction, String readService) {
            this.jurisdiction = jurisdiction;
            this.readService = readService;
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
        // endregion
    }
}
