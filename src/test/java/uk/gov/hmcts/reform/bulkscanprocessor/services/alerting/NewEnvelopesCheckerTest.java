package uk.gov.hmcts.reform.bulkscanprocessor.services.alerting;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.LoggerTestUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NewEnvelopesCheckerTest {

    private ListAppender<ILoggingEvent> loggingEvents;

    @Mock private EnvelopeRepository repo;
    @Mock private LocalDateTime now;

    private NewEnvelopesChecker checker;

    @BeforeEach
    void setUp() {
        loggingEvents = LoggerTestUtil.getListAppenderForClass(NewEnvelopesChecker.class);
        checker = new NewEnvelopesChecker(repo, () -> now);
    }

    @Test
    public void should_not_check_outside_of_specified_hours() {
        // given
        given(now.getDayOfWeek()).willReturn(MONDAY);
        given(now.getHour()).willReturn(NewEnvelopesChecker.END_HOUR + 1);

        // when
        checker.checkIfEnvelopesAreMissing();

        // then
        verify(repo, never()).countAllByCreatedAtAfter(any());
        assertThat(loggingEvents.list).isEmpty();
    }

    @Test
    public void should_not_check_on_weekend() {
        // given
        given(now.getDayOfWeek()).willReturn(SATURDAY);
        given(now.getHour()).willReturn(NewEnvelopesChecker.END_HOUR - 1);

        // when
        checker.checkIfEnvelopesAreMissing();

        // then
        verify(repo, never()).countAllByCreatedAtAfter(any());
        assertThat(loggingEvents.list).isEmpty();
    }

    @Test
    public void should_log_error_when_there_are_no_new_envelopes() {
        // given
        Instant instant = Instant.now();
        given(this.now.getDayOfWeek()).willReturn(MONDAY);
        given(this.now.getHour()).willReturn(NewEnvelopesChecker.END_HOUR - 1);
        given(this.now.toInstant(ZoneOffset.UTC)).willReturn(instant);

        given(repo.countAllByCreatedAtAfter(any())).willReturn(0);

        // when
        checker.checkIfEnvelopesAreMissing();

        // then
        verify(repo).countAllByCreatedAtAfter(instant.minus(NewEnvelopesChecker.TIME_WINDOW));
        assertThat(loggingEvents.list)
            .extracting(ILoggingEvent::getLevel)
            .contains(Level.ERROR);
    }

    @Test
    public void should_not_log_error_when_there_exists_at_least_one_new_envelope() {
        // given
        Instant instant = Instant.now();
        given(this.now.getDayOfWeek()).willReturn(MONDAY);
        given(this.now.getHour()).willReturn(NewEnvelopesChecker.END_HOUR - 1);
        given(this.now.toInstant(ZoneOffset.UTC)).willReturn(instant);

        given(repo.countAllByCreatedAtAfter(any())).willReturn(1);

        // when
        checker.checkIfEnvelopesAreMissing();

        // then
        verify(repo).countAllByCreatedAtAfter(instant.minus(NewEnvelopesChecker.TIME_WINDOW));
        assertThat(loggingEvents.list)
            .extracting(ILoggingEvent::getLevel)
            .doesNotContain(Level.ERROR);
    }
}
