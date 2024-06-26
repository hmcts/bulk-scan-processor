package uk.gov.hmcts.reform.bulkscanprocessor.config;

import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

/**
 * Configuration for clock.
 */
@Configuration
public class ClockConfig {

    /**
     * Bean for ClockProvider.
     * @return The ClockProvider
     */
    @Bean
    public ClockProvider clockProvider() {
        return () -> Clock.system(EUROPE_LONDON_ZONE_ID);
    }
}
