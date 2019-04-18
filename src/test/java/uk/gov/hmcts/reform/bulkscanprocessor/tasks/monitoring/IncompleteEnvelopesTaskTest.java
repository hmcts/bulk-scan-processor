package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

public class IncompleteEnvelopesTaskTest {

    private static final IncompleteEnvelopesTask TASK = new IncompleteEnvelopesTask();

    @Test
    public void should_finish_the_task_successfully_when_no_envelopes_are_present() {
        assertThatCode(TASK::run).doesNotThrowAnyException();
    }
}
