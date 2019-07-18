package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Can be used to check what gets logged in tests.
 */
public class StaticLogAppender extends AppenderBase<ILoggingEvent> {

    private static List<ILoggingEvent> events = new ArrayList<>();

    @Override
    public void append(ILoggingEvent e) {
        events.add(e);
    }

    public static List<ILoggingEvent> getEvents() {
        return events;
    }

    public static void clearEvents() {
        events.clear();
    }
}
