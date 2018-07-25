package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;

public class TaskErrorHandler implements ErrorHandler {

    private final EnvelopeRepository envelopeRepository;

    private final ProcessEventRepository eventRepository;

    public TaskErrorHandler(EnvelopeRepository envelopeRepository, ProcessEventRepository eventRepository) {
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void handleError(Throwable t) {
        // to be implemented
    }
}
