package uk.gov.hmcts.reform.bulkscanprocessor.util;

import java.time.ZoneId;

/**
 * Time zones used in the application.
 */
public final class TimeZones {

    public static final String EUROPE_LONDON = "Europe/London";
    public static final ZoneId EUROPE_LONDON_ZONE_ID = ZoneId.of(EUROPE_LONDON);

    /**
     * Constructs a new instance of time zones utility class.
     */
    private TimeZones() {
        // utility class construct
    }
}
