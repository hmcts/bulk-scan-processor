package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

import java.util.Collection;

public class FileNameIrregularitiesException extends RuntimeException implements EnvelopeAwareThrowable {

    private static final Event FAILURE_EVENT = Event.DOC_FAILURE;

    private final Envelope envelope;

    public FileNameIrregularitiesException(Envelope envelope, Collection<String> fileNames) {
        super("Missing PDFs: " + String.join(", ", fileNames));

        this.envelope = envelope;
    }

    @Override
    public Envelope getEnvelope() {
        return envelope;
    }

    @Override
    public Event getEvent() {
        return FAILURE_EVENT;
    }

    @Override
    public String getContainer() {
        return envelope.getContainer();
    }

    @Override
    public String getZipFileName() {
        return envelope.getZipFileName();
    }
}
