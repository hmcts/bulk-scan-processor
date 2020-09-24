package uk.gov.hmcts.reform.bulkscanprocessor.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomCheckHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.down().build();
    }

}
