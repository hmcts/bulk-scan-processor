package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class DocUploadFailureGenericException extends RuntimeException implements EnvelopeAwareThrowable {

    private static final Event DOC_UPLOAD_FAILURE_EVENT = Event.DOC_UPLOAD_FAILURE;

    private final transient Envelope envelope;

    public DocUploadFailureGenericException(Envelope envelope, Throwable cause) {
        super(cause.getMessage(), cause);

        this.envelope = envelope;
    }

    @Override
    public Event getEvent() {
        return DOC_UPLOAD_FAILURE_EVENT;
    }

    @Override
    public String getContainer() {
        return envelope.getContainer();
    }

    @Override
    public String getZipFileName() {
        return envelope.getZipFileName();
    }

    @Override
    public Envelope getEnvelope() {
        return envelope;
    }
}
