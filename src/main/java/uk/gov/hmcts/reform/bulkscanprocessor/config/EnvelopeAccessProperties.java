package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "envelope-access")
public class EnvelopeAccessProperties {
    private List<Mapping> mappings;

    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public static class Mapping {

        private String jurisdiction;
        private String readService;
        private String writeService;

        public Mapping(String jurisdiction, String readService, String writeService) {
            this.jurisdiction = jurisdiction;
            this.readService = readService;
            this.writeService = writeService;
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

        public String getWriteService() {
            return writeService;
        }

        public void setWriteService(String writeService) {
            this.writeService = writeService;
        }
    }
}
