package uk.gov.hmcts.reform.bulkscanprocessor.util;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PendingMigrationScriptException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.reset;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FlywayNoOpStrategyTest {

    @Mock
    private Flyway flyway;

    @Mock
    private MigrationInfoService infoService;

    @Mock
    private MigrationInfo info;

    private final FlywayMigrationStrategy strategy = new FlywayNoOpStrategy();

    @AfterAll
    public void tearUp() {
        reset(flyway, infoService, info);
    }

    @Test
    public void should_not_throw_exception_when_all_migrations_are_applied() {
        MigrationInfo[] infos = { info, info };
        given(flyway.info()).willReturn(infoService);
        given(infoService.all()).willReturn(infos);
        given(info.getState()).willReturn(MigrationState.SUCCESS);

        Throwable exception = catchThrowable(() -> strategy.migrate(flyway));
        assertThat(exception).isNull();
    }

    @Test
    public void should_throw_exception_when_one_migration_is_pending() {
        MigrationInfo[] infos = { info, info };
        given(flyway.info()).willReturn(infoService);
        given(infoService.all()).willReturn(infos);
        given(info.getState()).willReturn(MigrationState.SUCCESS, MigrationState.PENDING);

        Throwable exception = catchThrowable(() -> strategy.migrate(flyway));
        assertThat(exception)
            .isInstanceOf(PendingMigrationScriptException.class)
            .hasMessageStartingWith("Found migration not yet applied");
    }
}
