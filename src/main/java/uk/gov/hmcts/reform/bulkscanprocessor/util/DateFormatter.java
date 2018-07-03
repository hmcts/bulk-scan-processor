package uk.gov.hmcts.reform.bulkscanprocessor.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

final class DateFormatter {

    private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    static Timestamp getTimestamp(final String timestamp) throws ParseException {
        return new Timestamp(format.parse(timestamp).getTime());
    }

    static String getSimpleDateTime(final Timestamp timestamp) {
        return format.format(timestamp);
    }

    private DateFormatter() {
        // utility class constructor
    }
}
