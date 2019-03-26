package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    properties = {
        "spring.mail.host=false"
    }
)
@RunWith(SpringRunner.class)
public class ReportSenderDisabledTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void should_not_have_report_sender_in_context() {
        assertThat(context.getBeanNamesForType(ReportSender.class)).isEmpty();
    }
}
