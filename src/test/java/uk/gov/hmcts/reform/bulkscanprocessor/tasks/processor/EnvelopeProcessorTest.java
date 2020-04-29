package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;

@ExtendWith(MockitoExtension.class)
class EnvelopeProcessorTest {

    @Mock private MetafileJsonValidator schemaValidator;
    @Mock private EnvelopeRepository envelopeRepository;
    @Mock private ProcessEventRepository processEventRepository;

    private EnvelopeProcessor envelopeProcessor;

    @BeforeEach
    void setUp() {
        envelopeProcessor = new EnvelopeProcessor(schemaValidator, envelopeRepository, processEventRepository, 0, 0);
    }

    @Test
    void should_mark_as_upload_failure_and_increase_the_counter() {
        // given
        Envelope envelope = new Envelope();
        envelope.setStatus(CREATED);
        envelope.setUploadFailureCount(0);

        // when
        envelopeProcessor.markAsUploadFailure(envelope);

        // then
        ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository, times(1)).saveAndFlush(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue())
            .satisfies(actualEnvelope -> {
                assertThat(actualEnvelope.getStatus()).isEqualTo(UPLOAD_FAILURE);
                assertThat(actualEnvelope.getUploadFailureCount()).isEqualTo(1);
            });
    }

    @Test
    void should_create_new_event_only() {
        // given
        given(processEventRepository.saveAndFlush(any(ProcessEvent.class))).willReturn(new ProcessEvent());

        // when
        envelopeProcessor.createEvent(DOC_UPLOAD_FAILURE, "container", "zip-file-name", "reason", randomUUID());

        // then
        verify(processEventRepository, times(1)).saveAndFlush(any(ProcessEvent.class));
        verifyNoInteractions(schemaValidator, envelopeRepository);
    }
}
