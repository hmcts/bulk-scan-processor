package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IncompleteEnvelopesTaskTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    private IncompleteEnvelopesTask task;

    @BeforeEach
    void setUp() {
        task = new IncompleteEnvelopesTask(envelopeRepository, Duration.ofHours(1));
    }

    @Test
    void should_finish_the_task_successfully_when_no_envelopes_are_present() {
        given(envelopeRepository.getIncompleteEnvelopesCountBefore(any())).willReturn(0);

        assertThatCode(task::run).doesNotThrowAnyException();
    }

    @Test
    void should_finish_the_task_successfully_when_there_are_some_incomplete_envelopes() {
        given(envelopeRepository.getIncompleteEnvelopesCountBefore(any())).willReturn(1);

        assertThatCode(task::run).doesNotThrowAnyException();
    }
}
