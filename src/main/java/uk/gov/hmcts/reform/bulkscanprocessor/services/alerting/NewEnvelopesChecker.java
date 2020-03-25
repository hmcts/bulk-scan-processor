package uk.gov.hmcts.reform.bulkscanprocessor.services.alerting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;

public class NewEnvelopesChecker {

    private static final Logger logger = LoggerFactory.getLogger(NewEnvelopesChecker.class);

    public static final Duration TIME_WINDOW = Duration.ofHours(1);
    public static final int START_HOUR = 10;
    public static final int END_HOUR = 18;

    private final EnvelopeRepository repo;
    private final Supplier<LocalDateTime> timeSupplier;

    public NewEnvelopesChecker(EnvelopeRepository repo, Supplier<LocalDateTime> timeSupplier) {
        this.repo = repo;
        this.timeSupplier = timeSupplier;
    }

    public void checkIfEnvelopesAreMissing() {
        LocalDateTime now = timeSupplier.get();

        boolean isWeekend = asList(SATURDAY, SUNDAY).contains(now.getDayOfWeek());
        boolean isBusinessHours = START_HOUR <= now.getHour() && now.getHour() <= END_HOUR;

        if (!isWeekend && isBusinessHours) {
            logger.info("Checking if new envelopes have been received");

            Instant cutoff = now.toInstant(UTC).minus(TIME_WINDOW);
            if (repo.countAllByCreatedAtAfter(cutoff) == 0) {
                logger.error("No envelopes received since {}", cutoff);
            }
        }
    }
}
