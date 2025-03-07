package uk.gov.hmcts.reform.bulkscanprocessor.services.email;

import jakarta.mail.internet.MimeMessage;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Sends daily report to configured recipients.
 */
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class ReportSender {

    private static final Logger log = LoggerFactory.getLogger(ReportSender.class);

    public static final String EMAIL_SUBJECT = "Bulk Scan daily report";
    public static final String EMAIL_BODY = "This is an auto generated email. Do not respond to it.";
    public static final String ATTACHMENT_PREFIX = "Bulk-Scan-Daily-Report-";
    private final JavaMailSender mailSender;
    private final ReportsService reportsService;
    private final String from;
    private final String[] recipients;

    /**
     * Constructor for ReportSender.
     * @param mailSender The mail sender
     * @param reportsService The reports service
     * @param from The sender email address
     * @param recipients The recipients email addresses
     */
    public ReportSender(
        JavaMailSender mailSender,
        ReportsService reportsService,
        @Value("${spring.mail.username}") String from,
        @Value("${reports.recipients}") String[] recipients
    ) {
        this.mailSender = mailSender;
        this.reportsService = reportsService;
        this.from = from;

        if (recipients == null) {
            this.recipients = new String[0];
        } else {
            this.recipients = Arrays.copyOf(recipients, recipients.length);
        }

        if (this.recipients.length == 0) {
            log.warn("No recipients configured for reports");
        }
    }

    /**
     * Sends the daily report.
     */
    @Scheduled(cron = "${reports.cron}")
    @SchedulerLock(name = "report-sender")
    public void send() {
        try {
            log.info("Sending daily report: {}", EMAIL_SUBJECT);
            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(from);
            helper.setTo(this.recipients);
            helper.setSubject(EMAIL_SUBJECT);
            helper.setText(EMAIL_BODY);
            helper.addAttachment(ATTACHMENT_PREFIX + getPreviousDay() + ".csv", getCsvReport());

            mailSender.send(msg);

        } catch (Exception exc) {
            log.error("Error sending report", exc);
        }
    }

    /**
     * Gets the CSV report.
     * @return The CSV report
     * @throws IOException If an I/O error occurs
     */
    private File getCsvReport() throws IOException {
        List<ZipFileSummaryResponse> reportDate = reportsService.getZipFilesSummary(getPreviousDay(), null, null);
        return CsvWriter.writeZipFilesSummaryToCsv(reportDate);
    }

    /**
     * Gets the previous day.
     * @return The previous day
     */
    private LocalDate getPreviousDay() {
        return LocalDate.now().minusDays(1);
    }
}
