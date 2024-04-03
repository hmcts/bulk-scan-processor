package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for envelope access.
 */
@ConfigurationProperties(prefix = "envelope-access")
public class EnvelopeAccessProperties {

    private List<Mapping> mappings;

    /**
     * Get list of mappings.
     * @return The mappings
     */
    public List<Mapping> getMappings() {
        return mappings;
    }

    /**
     * Set list of mappings.
     * @param mappings The mappings
     */
    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    /**
     * Mapping model.
     */
    public static class Mapping {

        private String jurisdiction;
        private String readService;

        /**
         * Constructor.
         * @param jurisdiction The jurisdiction
         * @param readService The readService
         */
        public Mapping(String jurisdiction, String readService) {
            this.jurisdiction = jurisdiction;
            this.readService = readService;
        }

        /**
         * Constructor.
         */
        public Mapping() {
            // Spring needs it.
        }

        /**
         * Get jurisdiction.
         * @return The jurisdiction
         */
        public String getJurisdiction() {
            return jurisdiction;
        }

        /**
         * Set jurisdiction.
         * @param jurisdiction The jurisdiction
         */
        public void setJurisdiction(String jurisdiction) {
            this.jurisdiction = jurisdiction;
        }

        /**
         * Get read service.
         * @return The readService
         */
        public String getReadService() {
            return readService;
        }

        /**
         * Set read service.
         * @param readService The readService
         */
        public void setReadService(String readService) {
            this.readService = readService;
        }
    }
}
