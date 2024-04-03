package uk.gov.hmcts.reform.bulkscanprocessor.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

/**
 * Utility class for date formatting.
 */
public final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    /**
     * Returns the Instant from the given timestamp.
     *
     * @param timestamp the timestamp in the format of DATETIME_PATTERN
     * @return the Instant
     */
    static Instant getInstant(final String timestamp) {
        return LocalDateTime.parse(timestamp, formatter).toInstant(UTC);
    }

    /**
     * Returns the formatted date time string from the given Instant.
     *
     * @param instant the Instant
     * @return the formatted date time string
     */
    public static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.from(UTC)));
    }

    /**
     * Get the pattern used for date formatting.
     * @return the pattern
     */
    static String getPattern() {
        return DATETIME_PATTERN;
    }

    /**
     * Constructor for the DateFormatter.
     */
    private DateFormatter() {
        // utility class constructor
    }
}
