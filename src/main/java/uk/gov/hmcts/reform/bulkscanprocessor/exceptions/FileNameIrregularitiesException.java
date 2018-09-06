package uk.gov.hmcts.reform.bulkscanprocessor.exceptions;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;

import java.util.Collection;

public class FileNameIrregularitiesException extends RuntimeException implements EventRelatedThrowable {

    private static final Event FAILURE_EVENT = Event.DOC_FAILURE;

    private final transient Envelope envelope;

    public FileNameIrregularitiesException(Envelope envelope, Collection<String> fileNames) {
        super("Missing PDFs: " + String.join(", ", fileNames));

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
