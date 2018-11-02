package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

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
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper.ErrorHandlingWrapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class FailedDocUploadProcessorTest {

    @Rule
    public OutputCapture outputCapture = new OutputCapture();

    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ErrorHandlingWrapper errorWrapper;

    @Mock
    private ServiceBusHelper serviceBusHelper;

    private FailedDocUploadProcessor processor;

    @Before
    public void setUp() {
        processor = new FailedDocUploadProcessor(
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
        List<Envelope> envelopes = Collections.nCopies(3, testEnvelope);

        // and
        given(envelopeProcessor.getFailedToUploadEnvelopes("SSCS")).willReturn(envelopes);

        // we only test the output pattern. client is null
        // when
        catchThrowableOfType(() -> processor.processJurisdiction("SSCS"), NullPointerException.class);

        // then
        assertThat(outputCapture.toString()).containsPattern("Processing 3 failed documents for container test");
    }
}
