package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public interface EventRelatedThrowable {

    Event getEvent();

    String getContainer();

    String getZipFileName();
}
