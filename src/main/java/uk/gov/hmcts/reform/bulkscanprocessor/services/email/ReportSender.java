package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javax.mail.internet.MimeMessage;

@Component
@ConditionalOnBean(JavaMailSender.class)
public class ReportSender {

    private static final Logger log = LoggerFactory.getLogger(ReportSender.class);

    public static final String EMAIL_SUBJECT = "Bulk Scan daily report";
    public static final String EMAIL_BODY = "This is an auto generated email. Do not respond to it.";
    public static final String ATTACHMENT_PREFIX = "bulk_scan_envelopes_";

    private final JavaMailSender mailSender;
    private final ReportsService reportsService;
    private final String[] recipients;

    // region constructor
    public ReportSender(
        JavaMailSender mailSender,
        ReportsService reportsService,
        @Value("${reports.recipients}") String reportRecipients
    ) {
        this.mailSender = mailSender;
        this.reportsService = reportsService;
        this.recipients = reportRecipients.split(",");

        if (this.recipients.length == 0) {
            log.warn("No recipients configured for reports");
        }
    }
    // endregion

    public void send() {
        try {
            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setTo(this.recipients);
            helper.setSubject(EMAIL_SUBJECT);
            helper.setText(EMAIL_BODY);
            helper.addAttachment(ATTACHMENT_PREFIX + LocalDate.now(), getCsvReport());

            mailSender.send(msg);

        } catch (Exception exc) {
            log.error("Error sending report", exc);
        }
    }

    private File getCsvReport() throws IOException {
        List<ZipFileSummaryResponse> reportDate = reportsService.getZipFilesSummary(LocalDate.now(), null);
        return CsvWriter.writeZipFilesSummaryToCsv(reportDate);
    }
}
