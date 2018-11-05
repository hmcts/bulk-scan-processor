package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public abstract class EventRelatedThrowable extends RuntimeException {

    public EventRelatedThrowable(String message) {
        super(message);
    }

    public EventRelatedThrowable(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract Event getEvent();

    public abstract String getContainer();

    public abstract String getZipFileName();
}
