package uk.gov.hmcts.reform.bulkscanprocessor.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.sql.DataSource;

/**
 * Configuration for ShedLock.
 */
@Configuration
@AutoConfigureAfter(FlywayConfiguration.class)
@DependsOn({"flyway", "flywayInitializer"})
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "${scheduling.lock_at_most_for}")
public class ShedLockConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ShedLockConfiguration.class);

    /**
     * Bean for lock provider.
     * @param dataSource The DataSource
     * @return The LockProvider
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    /**
     * Bean for custom task scheduler.
     * @param poolSize The pool size
     * @return The TaskScheduler
     */
    @Bean
    public TaskScheduler customTaskScheduler(
        @Value("${scheduling.pool}") int poolSize
    ) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("BSP-");
        scheduler.setErrorHandler(error -> log.error("The execution of a scheduled job failed", error));
        scheduler.initialize();

        return scheduler;
    }
}
