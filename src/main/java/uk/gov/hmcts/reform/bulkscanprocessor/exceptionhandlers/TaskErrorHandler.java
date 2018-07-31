package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeAwareThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedThrowable;

public class TaskErrorHandler implements ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskErrorHandler.class);

    private final EnvelopeRepository envelopeRepository;

    private final ProcessEventRepository eventRepository;

    public TaskErrorHandler(EnvelopeRepository envelopeRepository, ProcessEventRepository eventRepository) {
        this.envelopeRepository = envelopeRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void handleError(Throwable t) {
        if (t instanceof EnvelopeAwareThrowable) {
            updateEnvelopeLastStatus((EnvelopeAwareThrowable) t);
        }

        if (t instanceof EventRelatedThrowable) {
            registerEvent((EventRelatedThrowable) t, t.getMessage());
        }

        log.error(t.getMessage(), t);
    }

    private void updateEnvelopeLastStatus(EnvelopeAwareThrowable throwable) {
        Status.fromEvent(throwable.getEvent()).ifPresent(status -> {
            Envelope envelope = throwable.getEnvelope();
            envelope.setStatus(status);

            envelopeRepository.save(envelope);
        });
    }

    private void registerEvent(EventRelatedThrowable exception, String reason) {
        ProcessEvent processEvent = new ProcessEvent(
            exception.getContainer(),
            exception.getZipFileName(),
            exception.getEvent()
        );

        processEvent.setReason(reason);
        eventRepository.save(processEvent);
    }
}
