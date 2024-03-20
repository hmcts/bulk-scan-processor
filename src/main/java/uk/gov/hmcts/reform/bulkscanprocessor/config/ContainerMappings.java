package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for container mappings.
 */
@ConfigurationProperties(prefix = "containers")
public class ContainerMappings {
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

        private String container;
        private String jurisdiction;
        private List<String> poBoxes;
        private String ocrValidationUrl;
        private boolean paymentsEnabled;
        private boolean enabled = true;

        /**
         * Constructor.
         * @param container The container
         * @param jurisdiction The jurisdiction
         * @param poBoxes The poBoxes
         * @param ocrValidationUrl The ocrValidationUrl
         * @param paymentsEnabled The paymentsEnabled
         * @param enabled The enabled
         */
        public Mapping(
            String container,
            String jurisdiction,
            List<String> poBoxes,
            String ocrValidationUrl,
            boolean paymentsEnabled,
            boolean enabled
        ) {
            this.container = container;
            this.jurisdiction = jurisdiction;
            this.poBoxes = poBoxes;
            this.ocrValidationUrl = ocrValidationUrl;
            this.paymentsEnabled = paymentsEnabled;
            this.enabled = enabled;
        }

        /**
         * Constructor.
         */
        public Mapping() {
            // Spring needs it.
        }

        /**
         * Get container.
         * @return The container
         */
        public String getContainer() {
            return container;
        }

        /**
         * Set container.
         * @param container The container
         */
        public void setContainer(String container) {
            this.container = container;
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
         * Get poBoxes.
         * @return The poBoxes
         */
        public List<String> getPoBoxes() {
            return poBoxes;
        }

        /**
         * Set poBoxes.
         * @param poBoxes The poBoxes
         */
        public void setPoBoxes(List<String> poBoxes) {
            this.poBoxes = poBoxes;
        }

        /**
         * Get ocrValidationUrl.
         * @return The ocrValidationUrl
         */
        public String getOcrValidationUrl() {
            return ocrValidationUrl;
        }

        /**
         * Set ocrValidationUrl.
         * @param ocrValidationUrl The ocrValidationUrl
         */
        public void setOcrValidationUrl(String ocrValidationUrl) {
            this.ocrValidationUrl = ocrValidationUrl;
        }

        /**
         * Get paymentsEnabled.
         * @return The paymentsEnabled
         */
        public boolean isPaymentsEnabled() {
            return paymentsEnabled;
        }

        /**
         * Set paymentsEnabled.
         * @param paymentsEnabled The paymentsEnabled
         */
        public void setPaymentsEnabled(boolean paymentsEnabled) {
            this.paymentsEnabled = paymentsEnabled;
        }

        /**
         * Get enabled.
         * @return The enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Set enabled.
         * @param enabled The enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
