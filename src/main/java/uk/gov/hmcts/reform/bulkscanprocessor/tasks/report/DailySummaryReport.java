package uk.gov.hmcts.reform.bulkscanprocessor.tasks.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.bulkscanprocessor.services.email.ReportSender;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class DailySummaryReport {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryReport.class);

    // public static final String EMAIL_SUBJECT = "Bulk Scan daily report";
    // public static final String ATTACHMENT_PREFIX = "bulk_scan_envelopes_";

    private final ReportsService service;

    private final ReportSender emailSender;

    public DailySummaryReport(
        ReportsService service,
        ReportSender emailSender
    ) {
        this.service = service;
        this.emailSender = emailSender;
    }

    public void send() {
        try {
            List<ZipFileSummaryResponse> reportDate = service.getZipFilesSummary(LocalDate.now(), null);

            File report = CsvWriter.writeZipFilesSummaryToCsv(reportDate);

            // will include subject, filename and report
            // once schedule is moved here
            emailSender.send();
        } catch (IOException exc) {
            log.error("Error generating daily summary report", exc);
        }
    }
}
