package uk.gov.hmcts.reform.bulkscanprocessor.services.alerting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.util.Arrays.asList;

/**
 * Checks if new envelopes have been received within the last hour.
 */
@Component
public class NewEnvelopesChecker {

    private static final Logger logger = LoggerFactory.getLogger(NewEnvelopesChecker.class);

    public static final Duration TIME_WINDOW = Duration.ofHours(1);
    public static final int START_HOUR = 10;
    public static final int END_HOUR = 18;

    private final EnvelopeRepository repo;
    private final Supplier<ZonedDateTime> timeSupplier;

    /**
     * Constructor for the NewEnvelopesChecker.
     * @param repo The repository for envelopes
     * @param timeSupplier The supplier for the current time
     */
    public NewEnvelopesChecker(EnvelopeRepository repo, Supplier<ZonedDateTime> timeSupplier) {
        this.repo = repo;
        this.timeSupplier = timeSupplier;
    }

    /**
     * Checks if new envelopes have been received within the last hour.
     */
    public void checkIfEnvelopesAreMissing() {
        ZonedDateTime now = timeSupplier.get();

        boolean isWeekend = asList(SATURDAY, SUNDAY).contains(now.getDayOfWeek());
        boolean isBusinessHours = START_HOUR <= now.getHour() && now.getHour() <= END_HOUR;

        if (!isWeekend && isBusinessHours) {
            logger.info("Checking if new envelopes have been received");

            Instant cutoff = now.toInstant().minus(TIME_WINDOW);
            if (repo.countAllByCreatedAtAfter(cutoff) == 0) {
                logger.error("No envelopes received since {}", cutoff);
            }
        }
    }
}
