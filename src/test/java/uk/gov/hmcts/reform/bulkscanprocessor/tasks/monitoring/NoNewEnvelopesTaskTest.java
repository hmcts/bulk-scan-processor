package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.services.alerting.NewEnvelopesChecker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NoNewEnvelopesTaskTest {

    @Test
    void should_call_checker_service() {
        // given
        NewEnvelopesChecker checker = mock(NewEnvelopesChecker.class);
        NoNewEnvelopesTask task = new NoNewEnvelopesTask(checker);

        // when
        task.run();

        // then
        verify(checker, times(1)).checkIfEnvelopesAreMissing();
    }
}
