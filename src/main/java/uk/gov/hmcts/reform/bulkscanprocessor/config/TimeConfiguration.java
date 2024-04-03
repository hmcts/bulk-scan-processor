package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON;

/**
 * Configuration for time related beans.
 */
@Configuration
public class TimeConfiguration {

    /**
     * Bean for timeSupplier.
     * @return The timeSupplier
     */
    @Bean
    public Supplier<ZonedDateTime> timeSupplier() {
        return () -> ZonedDateTime.now(ZoneId.of(EUROPE_LONDON));
    }
}
