package uk.gov.hmcts.reform.bulkscanprocessor.tasks.report;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.email.ReportSender;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;

import javax.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@IntegrationTest
@RunWith(SpringRunner.class)
public class DailySummaryReportTest {

    @Autowired
    private ReportSender emailSender;

    @Autowired
    private ReportsService service;

    @SpyBean
    private JavaMailSender mailSender;

    // will be autowired later
    private DailySummaryReport summaryReport;

    @Before
    public void setUp() {
        summaryReport = new DailySummaryReport(service, emailSender);
    }

    @Test
    public void should_attempt_to_send_report_when_recipients_list_is_present() {
        summaryReport.send();

        verify(mailSender).send(any(MimeMessage.class));
    }
}
