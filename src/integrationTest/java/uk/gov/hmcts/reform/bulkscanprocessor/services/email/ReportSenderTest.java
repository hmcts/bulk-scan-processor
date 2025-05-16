package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@IntegrationTest
public class ReportSenderTest {

    @Autowired
    private ReportSender reportSender;

    @MockitoSpyBean
    private JavaMailSender mailSender;

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() {
        reportSender.send();

        verify(mailSender).send(any(MimeMessage.class));
    }
}
