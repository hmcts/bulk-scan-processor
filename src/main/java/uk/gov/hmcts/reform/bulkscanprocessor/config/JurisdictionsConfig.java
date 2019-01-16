package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Configuration
public class JurisdictionsConfig {

    private final EnvelopeAccessProperties envelopeAccessProperties;

    public JurisdictionsConfig(EnvelopeAccessProperties envelopeAccessProperties) {
        this.envelopeAccessProperties = envelopeAccessProperties;
    }

    @Bean(name = "jurisdictions")
    public Set<String> getKnownJurisdictions() {
        return envelopeAccessProperties
            .getMappings()
            .stream()
            .map(Mapping::getJurisdiction)
            .collect(toSet());
    }
}
