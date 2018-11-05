package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class PreviouslyFailedToUploadException extends EventRelatedThrowable {

    private static final Event DOC_UPLOAD_FAILURE_EVENT = Event.DOC_UPLOAD_FAILURE;

    private final String container;

    private final String zipFileName;

    public PreviouslyFailedToUploadException(String container, String zipFileName, String message) {
        super(message);

        this.container = container;
        this.zipFileName = zipFileName;
    }

    @Override
    public Event getEvent() {
        return DOC_UPLOAD_FAILURE_EVENT;
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
