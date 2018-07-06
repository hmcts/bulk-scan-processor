package uk.gov.hmcts.reform.bulkscanprocessor.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import net.javacrumbs.shedlock.spring.ScheduledLockConfiguration;
import net.javacrumbs.shedlock.spring.ScheduledLockConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.time.Duration;
import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter(FlywayConfiguration.class)
@DependsOn({"flyway", "flywayInitializer"})
public class ShedLockConfiguration {

    @Value("${scheduling.pool}")
    private int poolSize;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public ScheduledLockConfiguration taskScheduler(LockProvider lockProvider) {
        return ScheduledLockConfigurationBuilder
            .withLockProvider(lockProvider)
            .withPoolSize(poolSize)
            .withDefaultLockAtMostFor(Duration.ofMinutes(10))
            .build();
    }
}
