package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PendingMigrationScriptException;

import java.util.stream.Stream;

/**
 * Flyway migration strategy that throws an exception if there are pending migrations.
 */
public class FlywayNoOpStrategy implements FlywayMigrationStrategy {

    /**
     * Throws an exception if there are pending migrations.
     *
     * @param flyway the Flyway instance
     */
    @Override
    public void migrate(Flyway flyway) {
        Stream.of(flyway.info().all())
            .filter(info -> !info.getState().isApplied())
            .findFirst()
            .ifPresent(info -> {
                throw new PendingMigrationScriptException(info.getScript());
            });
    }
}
