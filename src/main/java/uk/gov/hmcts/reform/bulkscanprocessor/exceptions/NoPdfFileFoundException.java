package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class NoPdfFileFoundException extends RuntimeException implements EventRelatedThrowable {

    private static final long serialVersionUID = 9143161748679833084L;

    private static final Event DOC_FAILURE_EVENT = Event.DOC_FAILURE;

    private final String container;

    private final String zipFileName;

    public NoPdfFileFoundException(String container, String zipFileName) {
        super("Zip file '" + zipFileName + "' does not contain pdf files");

        this.container = container;
        this.zipFileName = zipFileName;
    }

    @Override
    public Event getEvent() {
        return DOC_FAILURE_EVENT;
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
