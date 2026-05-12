package uk.gov.hmcts.reform.bulkscanprocessor.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.bulkscanprocessor.util.FlywayNoOpStrategy;

/**
 * Flyway configuration.
 */
@AutoConfigureAfter({
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@AutoConfigureBefore({
    FlywayAutoConfiguration.class
})
@Configuration
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfiguration {

    /**
     * Bean for FlywayMigrationStrategy.
     * @return The FlywayMigrationStrategy
     */
    @Bean
    @ConditionalOnProperty(prefix = "flyway.noop", name = "strategy", matchIfMissing = true)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayNoOpStrategy();
    }

    /**
     * Bean for FlywayMigrationStrategy.
     * @return The FlywayMigrationStrategy
     */
    @Bean
    @ConditionalOnProperty(prefix = "flyway.noop", name = "strategy", havingValue = "false")
    public FlywayMigrationStrategy flywayVoidMigrationStrategy() {
        return null;
    }
}
