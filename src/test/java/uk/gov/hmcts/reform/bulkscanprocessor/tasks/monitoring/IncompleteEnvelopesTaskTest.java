package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class IncompleteEnvelopesTaskTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private IncompleteEnvelopesTask task;

    @Before
    public void setUp() {
        task = new IncompleteEnvelopesTask(envelopeRepository);
    }

    @Test
    public void should_finish_the_task_successfully_when_no_envelopes_are_present() {
        given(envelopeRepository.getIncompleteEnvelopesCountBefore(now())).willReturn(0);

        assertThatCode(task::run).doesNotThrowAnyException();
    }

    @Test
    public void should_finish_the_task_successfully_when_there_are_some_incomplete_envelopes() {
        given(envelopeRepository.getIncompleteEnvelopesCountBefore(now())).willReturn(1);

        assertThatCode(task::run).doesNotThrowAnyException();
    }
}
