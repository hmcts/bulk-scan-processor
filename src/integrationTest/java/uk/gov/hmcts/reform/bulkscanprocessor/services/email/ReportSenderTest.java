package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    properties = {
        "spring.mail.host=smtp.server.com", // once present in config can be deleted
        "reports.recipients=integration@test"
    }
)
@RunWith(SpringRunner.class)
public class ReportSenderTest {

    @Autowired
    private ReportSender reportSender;

    @SpyBean
    private JavaMailSender mailSender;

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() {
        reportSender.send();

        verify(mailSender).send(any(MimeMessage.class));
    }
}
