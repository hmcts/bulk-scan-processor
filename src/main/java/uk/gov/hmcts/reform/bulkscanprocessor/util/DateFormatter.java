package uk.gov.hmcts.reform.bulkscanprocessor.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneOffset.UTC;

public final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    static Instant getInstant(final String timestamp) {
        return LocalDateTime.parse(timestamp, formatter).toInstant(UTC);
    }

    public static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.from(UTC)));
    }

    static String getPattern() {
        return DATETIME_PATTERN;
    }

    private DateFormatter() {
        // utility class constructor
    }
}
