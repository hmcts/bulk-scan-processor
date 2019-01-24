package uk.gov.hmcts.reform.bulkscanprocessor.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class TestAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new ArrayList<>();

    public TestAppender() {
        super();
        start();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(eventObject);
    }

    public List<ILoggingEvent> getEvents() {
        return events;
    }
}
