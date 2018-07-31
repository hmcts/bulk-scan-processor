package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class DocFailureGenericException extends RuntimeException implements EventRelatedThrowable {

    private static final Event EVENT = Event.DOC_FAILURE;

    private final String container;

    private final String zipFileName;

    public DocFailureGenericException(String container, String zipFileName, Throwable cause) {
        super(cause.getMessage(), cause);

        this.container = container;
        this.zipFileName = zipFileName;
    }

    @Override
    public Event getEvent() {
        return EVENT;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public String getZipFileName() {
        return zipFileName;
    }
}
