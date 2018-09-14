package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class DocProcessedNotificationFailureException extends RuntimeException
    implements EnvelopeAwareThrowable, EventRelatedThrowable {


    private final transient Envelope envelope;

    public DocProcessedNotificationFailureException(Envelope envelope) {
        super("Message send failure");

        this.envelope = envelope;
    }

    public DocProcessedNotificationFailureException(Envelope envelope, Throwable cause) {
        super(cause.getMessage(), cause);

        this.envelope = envelope;
    }

    @Override
    public Event getEvent() {
        return Event.DOC_PROCESSED_NOTIFICATION_FAILURE;
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
