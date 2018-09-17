package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class BlobDeleteFailureException extends RuntimeException implements EventRelatedThrowable {


    private final transient Envelope envelope;

    public BlobDeleteFailureException(Envelope envelope) {
        super("Blob delete failure");

        this.envelope = envelope;
    }

    public BlobDeleteFailureException(Envelope envelope, Throwable cause) {
        super(cause.getMessage(), cause);

        this.envelope = envelope;
    }

    @Override
    public Event getEvent() {
        return Event.BLOB_DELETE_FAILURE;
    }

    @Override
    public String getContainer() {
        return envelope.getContainer();
    }

    @Override
    public String getZipFileName() {
        return envelope.getZipFileName();
    }

    public Envelope getEnvelope() {
        return envelope;
    }
}
