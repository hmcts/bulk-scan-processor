package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "containers")
public class ContainerMappings {
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

        private String container;
        private String jurisdiction;
        private List<String> poBoxes;
        private String ocrValidationUrl;
        private boolean paymentsEnabled;
        private boolean enabled = true;

        // region constructor, getters and setters
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

        public Mapping() {
            // Spring needs it.
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }

        public String getJurisdiction() {
            return jurisdiction;
        }

        public void setJurisdiction(String jurisdiction) {
            this.jurisdiction = jurisdiction;
        }

        public List<String> getPoBoxes() {
            return poBoxes;
        }

        public void setPoBoxes(List<String> poBoxes) {
            this.poBoxes = poBoxes;
        }

        public String getOcrValidationUrl() {
            return ocrValidationUrl;
        }

        public void setOcrValidationUrl(String ocrValidationUrl) {
            this.ocrValidationUrl = ocrValidationUrl;
        }

        public boolean isPaymentsEnabled() {
            return paymentsEnabled;
        }

        public void setPaymentsEnabled(boolean paymentsEnabled) {
            this.paymentsEnabled = paymentsEnabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        // endregion
    }
}
