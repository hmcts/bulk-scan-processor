package uk.gov.hmcts.reform.bulkscanprocessor.tasks.monitoring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "monitoring.incomplete-envelopes.enabled=false"
})
public class IncompleteEnvelopesDisabledTaskTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(IncompleteEnvelopesTask.class)).isEmpty();
    }
}
