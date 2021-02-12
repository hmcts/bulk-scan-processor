package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.ZIP_PROCESSING_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;

@ExtendWith(MockitoExtension.class)
class ZipFileFailureHandlerTest {
    private ZipFileFailureHandler zipFileFailureHandler;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @BeforeEach
    void setUp() {
        zipFileFailureHandler = new ZipFileFailureHandler(envelopeProcessor, envelopeRepository);
    }

    @Test
    void should_create_event_and_dummy_envelope() {
        // given
        Exception ex = new Exception("msg");
        final String zipFileName = "file1.zip";
        final String container = "container";

        // when
        zipFileFailureHandler.handleZipFileFailure(container, zipFileName, ex);

        // then
        verify(envelopeProcessor).createEvent(DOC_FAILURE, container, zipFileName, ex.getMessage(), null);
        ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).saveAndFlush(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().getZipFileName()).isEqualTo(zipFileName);
        assertThat(envelopeCaptor.getValue().getContainer()).isEqualTo(container);
        assertThat(envelopeCaptor.getValue().getStatus()).isEqualTo(ZIP_PROCESSING_FAILURE);
    }
}
