package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.apache.commons.mail.util.MimeMessageParser;
import org.assertj.core.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;

import java.util.Properties;
import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReportSenderTest {

    private static final String TEST_LOGIN = "test@localhost.com";
    private static final String TEST_PASSWORD = "test_password";

    @Rule
    public final GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

    @Test
    public void should_send_email_to_all_recipients() throws Exception {
        // given
        String[] recipients = Arrays.array(
            "foo@hmcts.net",
            "bar@hmcts.net"
        );

        ReportsService reportsService = mock(ReportsService.class);

        greenMail.setUser(TEST_LOGIN, TEST_PASSWORD);
        ReportSender reportSender = new ReportSender(getMailSender(), reportsService, recipients);

        // when
        reportSender.send();

        // then
        MimeMessageParser msg = new MimeMessageParser(greenMail.getReceivedMessages()[0]).parse();

        assertThat(msg.getTo()).extracting(Address::toString).containsExactlyElementsOf(asList(recipients));
        assertThat(msg.getSubject()).isEqualTo(ReportSender.EMAIL_SUBJECT);
        assertThat(msg.getPlainContent()).isEqualTo(ReportSender.EMAIL_BODY);
        assertThat(msg.getAttachmentList()).hasSize(1);
        assertThat(msg.getAttachmentList().get(0).getName()).isEqualTo(ReportSender.ATTACHMENT_PREFIX + now());

        verify(reportsService).getZipFilesSummary(now(), null);
    }

    @Test
    public void should_handle_mail_exception() throws Exception {
        // given
        ReportsService reportsService = mock(ReportsService.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());

        willThrow(MailSendException.class)
            .given(mailSender)
            .send(any(MimeMessage.class));

        ReportSender reportSender = new ReportSender(mailSender, reportsService, new String[0]);

        // when
        Throwable exc = catchThrowable(reportSender::send);

        // then
        assertThat(exc).isNull();
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private JavaMailSender getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(ServerSetupTest.SMTP.getPort());
        mailSender.setUsername(TEST_LOGIN);
        mailSender.setPassword(TEST_PASSWORD);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", ServerSetupTest.SMTP.getProtocol());

        return mailSender;
    }
}
