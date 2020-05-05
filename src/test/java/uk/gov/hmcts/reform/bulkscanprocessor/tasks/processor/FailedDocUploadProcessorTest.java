package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class FailedDocUploadProcessorTest {

    @RegisterExtension
    public LogCapturer capturer = LogCapturer.create().captureForType(FailedDocUploadProcessor.class);

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ZipFileProcessor zipFileProcessor;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    private FailedDocUploadProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new FailedDocUploadProcessor(
            null,
            documentProcessor,
            envelopeProcessor,
            zipFileProcessor,
            envelopeRepository,
            processEventRepository,
            1 // max re-upload-tries-count
        );
    }

    @Test
    public void should_log_how_many_envelopes_processing_for_which_container() throws Exception {
        // given
        Envelope testEnvelope = EnvelopeCreator.envelope();
        testEnvelope.setContainer("test");
        List<Envelope> envelopes = Collections.nCopies(3, testEnvelope);

        // and
        given(envelopeRepository.findEnvelopesToResend(1)).willReturn(envelopes);

        // we only test the output pattern. client is null
        // when
        catchThrowableOfType(() -> processor.processJurisdiction(), NullPointerException.class);

        // then
        capturer.assertContains("Re-uploading documents. Container: test, envelopes found: 3");
        capturer.assertContains("Finished re-uploaded documents. Container: test");
    }
}
