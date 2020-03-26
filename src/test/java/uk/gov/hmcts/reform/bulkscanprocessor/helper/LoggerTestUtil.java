package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

public final class LoggerTestUtil {

    private LoggerTestUtil() {
        // util class
    }

    public static ListAppender<ILoggingEvent> getListAppenderForClass(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();

        logger.addAppender(appender);

        return appender;
    }
}
