package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

public class FileNameIrregularitiesException extends RuntimeException implements EventRelatedThrowable {

    private static final Event FAILURE_EVENT = Event.DOC_FAILURE;

    private final transient Envelope envelope;

    public FileNameIrregularitiesException(Envelope envelope, String message) {
        super(message);

        this.envelope = envelope;
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
