package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOAD_FAILURE;

@ExtendWith(MockitoExtension.class)
class EnvelopeProcessorTest {

    @Mock private MetafileJsonValidator schemaValidator;
    @Mock private EnvelopeRepository envelopeRepository;
    @Mock private ProcessEventRepository processEventRepository;

    @Test
    void should_create_new_event_only() {
        // given
        EnvelopeProcessor envelopeProcessor = new EnvelopeProcessor(
            schemaValidator,
            envelopeRepository,
            processEventRepository,
            0,
            0
        );

        // when
        envelopeProcessor.createEvent(DOC_UPLOAD_FAILURE, "container", "zip-file-name", "reason", randomUUID());

        // then
        verify(processEventRepository, times(1)).saveAndFlush(any(ProcessEvent.class));
        verifyNoInteractions(schemaValidator, envelopeRepository);
    }
}
