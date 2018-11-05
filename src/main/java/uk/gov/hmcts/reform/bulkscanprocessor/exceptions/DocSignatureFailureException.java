package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class DocSignatureFailureException extends EventRelatedThrowable {

    private final String container;

    private final String zipFileName;

    public DocSignatureFailureException(String container, String zipFileName, String message) {
        super(message);
        this.container = container;
        this.zipFileName = zipFileName;
    }

    @Override
    public Event getEvent() {
        return Event.DOC_SIGNATURE_FAILURE;
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
