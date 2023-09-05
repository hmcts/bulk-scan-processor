package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@TestPropertySource(
    properties = {
        "spring.mail.host=false"
    }
)
public class ReportSenderDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Disabled
    @Test
    public void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(ReportSender.class)).isEmpty();
    }
}
