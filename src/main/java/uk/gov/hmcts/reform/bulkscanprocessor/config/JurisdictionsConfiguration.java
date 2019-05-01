package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Configuration
public class JurisdictionsConfiguration {

    @Bean(name = "jurisdictions")
    public Set<String> jurisdictions(ContainerMappings containerMappings) {
        return containerMappings
            .getMappings()
            .stream()
            .map(m -> m.getJurisdiction())
            .collect(toSet());
    }
}
