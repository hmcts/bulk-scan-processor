package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import jakarta.validation.ClockProvider;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@Configuration
public class ClockConfig {

    @Bean
    public ClockProvider clockProvider() {
        return () -> Clock.system(EUROPE_LONDON_ZONE_ID);
    }
}
