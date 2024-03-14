package uk.gov.hmcts.reform.bulkscanprocessor.config;

import jakarta.validation.ClockProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@TestConfiguration
public class TestClockProvider {

    public static Instant stoppedInstant = Instant.now();

    @Bean
    @Primary
    public ClockProvider stoppedClock() {
        return () -> provideClock(TimeZones.EUROPE_LONDON_ZONE_ID);
    }

    private Clock provideClock(ZoneId zoneId) {
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zoneId;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return provideClock(zoneId);
            }

            @Override
            public Instant instant() {
                if (stoppedInstant == null) {
                    return ZonedDateTime.now(zoneId).toInstant();
                } else {
                    return stoppedInstant;
                }
            }
        };
    }
}
