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
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers.TaskErrorHandler;

import java.time.Duration;
import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter(FlywayConfiguration.class)
@DependsOn({"flyway", "flywayInitializer"})
public class ShedLockConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public TaskErrorHandler errorHandler(
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository
    ) {
        return new TaskErrorHandler(envelopeRepository, processEventRepository);
    }

    @Bean
    public TaskScheduler customTaskScheduler(
        TaskErrorHandler errorHandler,
        @Value("${scheduling.pool}") int poolSize
    ) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("BSP-");
        scheduler.setErrorHandler(errorHandler);
        scheduler.initialize();

        return scheduler;
    }

    @Bean
    public ScheduledLockConfiguration taskScheduler(
        LockProvider lockProvider,
        TaskScheduler taskScheduler,
        @Value("${scheduling.lock_at_most_for}") String lockAtMostFor
    ) {
        return ScheduledLockConfigurationBuilder
            .withLockProvider(lockProvider)
            .withTaskScheduler(taskScheduler)
            .withDefaultLockAtMostFor(Duration.parse(lockAtMostFor))
            .build();
    }
}
