package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import uk.gov.hmcts.reform.bulkscanprocessor.jupiter.GreenMailExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;

import java.time.LocalDate;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportSenderTest {

    private static final String TEST_LOGIN = "test@localhost.com";
    private static final String TEST_PASSWORD = "test_password";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Test
    void should_send_email_to_all_recipients() throws Exception {
        // given
        String reportRecipient1 = "Foo <foo@hmcts.net>";
        String reportRecipient2 = "bar@hmcts.net";

        ReportsService reportsService = mock(ReportsService.class);

        greenMail.setUser(TEST_LOGIN, TEST_PASSWORD);
        ReportSender reportSender = new ReportSender(
            getMailSender(),
            reportsService,
            TEST_LOGIN,
            new String[] { reportRecipient1, reportRecipient2}
        );

        // when
        reportSender.send();

        // then

        MimeMessage msg = greenMail.getReceivedMessages()[0];
        Multipart mainMultipart = (Multipart) msg.getContent();


        assertThat(msg.getAllRecipients())
            .extracting(Address::toString)
            .hasSize(2)
            .containsExactly(
                reportRecipient1,
                reportRecipient2
            );
        assertThat(msg.getSubject()).isEqualTo(ReportSender.EMAIL_SUBJECT);
        assertThat(mainMultipart.getBodyPart(0)).isEqualTo(ReportSender.EMAIL_BODY);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        verify(reportsService).getZipFilesSummary(yesterday, null, null);
    }

    @Test
    void should_handle_mail_exception() {
        // given
        ReportsService reportsService = mock(ReportsService.class);
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());

        willThrow(MailSendException.class)
            .given(mailSender)
            .send(any(MimeMessage.class));

        ReportSender reportSender = new ReportSender(mailSender, reportsService, TEST_LOGIN, null);

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
