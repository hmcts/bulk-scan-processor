package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.DocumentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class ReuploadFailedEnvelopeTaskTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ErrorHandlingWrapper errorWrapper;

    private ReuploadFailedEnvelopeTask task;

    @Before
    public void setUp() {
        task = new ReuploadFailedEnvelopeTask(
            null,
            documentProcessor,
            envelopeProcessor,
            errorWrapper
        );
    }

    @After
    public void tearDown() {
        outputCapture.flush();
    }

    @Test
    public void should_log_how_many_envelopes_processing_for_which_container() throws Exception {
        // given
        Envelope testEnvelope = EnvelopeCreator.envelope();
        testEnvelope.setContainer("test");
        Envelope dummyEnvelope = EnvelopeCreator.envelope();
        dummyEnvelope.setContainer("dummy");

        // and
        List<Envelope> envelopes = Stream.concat(
            Collections.nCopies(3, testEnvelope).stream(),
            Collections.nCopies(2, dummyEnvelope).stream()
        ).collect(Collectors.toList());

        given(envelopeProcessor.getFailedToUploadEnvelopes()).willReturn(envelopes);

        // when
        task.processUploadFailures();

        // then
        assertThat(outputCapture.toString())
            .containsPattern("Processing 3 failed documents for container test")
            .containsPattern("Processing 2 failed documents for container dummy");
    }
}
