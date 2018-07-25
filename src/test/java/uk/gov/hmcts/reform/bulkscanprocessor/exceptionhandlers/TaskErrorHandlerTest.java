package uk.gov.hmcts.reform.bulkscanprocessor.exceptionhandlers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeAwareThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;

@RunWith(MockitoJUnitRunner.class)
public class TaskErrorHandlerTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository eventRepository;

    private TaskErrorHandler handler;

    @Before
    public void setUp() {
        handler = new TaskErrorHandler(envelopeRepository, eventRepository);
    }

    @Test
    public void should_update_all_entities_when_envelope_aware_exception_is_thrown() {
        // given
        Envelope envelope = mock(Envelope.class);
        ArgumentCaptor<ProcessEvent> eventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        Throwable envelopeException = new EnvelopeException(envelope, DOC_UPLOAD_FAILURE);

        // when
        handler.handleError(envelopeException);

        // then
        verify(envelopeRepository).save(envelope);
        verify(eventRepository).save(eventCaptor.capture());

        // and
        assertThat(eventCaptor.getValue().getEvent()).isEqualByComparingTo(DOC_UPLOAD_FAILURE);
    }

    @Test
    public void should_only_create_event_when_event_aware_exception_is_thrown() {
        // given
        ArgumentCaptor<ProcessEvent> eventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        Throwable eventException = new EventException(DOC_FAILURE, "container", "zip");

        // when
        handler.handleError(eventException);

        // then
        verify(envelopeRepository, never()).save(any(Envelope.class));
        verify(eventRepository).save(eventCaptor.capture());

        // and
        assertThat(eventCaptor.getValue())
            .extracting("event", "container", "zipFileName")
            .contains(DOC_FAILURE, "container", "zip");
    }

    private static class EnvelopeException extends RuntimeException implements EnvelopeAwareThrowable {

        private final Envelope envelope;

        private final Event event;

        EnvelopeException(Envelope envelope, Event event) {
            super("oh no");

            this.envelope = envelope;
            this.event = event;
        }

        @Override
        public Envelope getEnvelope() {
            return envelope;
        }

        @Override
        public Event getEvent() {
            return event;
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

    private static class EventException extends RuntimeException implements EventRelatedException {

        private final Event event;

        private final String container;

        private final String zipFileName;

        EventException(Event event, String container, String zipFileName) {
            super("oh no");

            this.event = event;
            this.container = container;
            this.zipFileName = zipFileName;
        }

        @Override
        public Event getEvent() {
            return event;
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
}
