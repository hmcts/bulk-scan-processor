package uk.gov.hmcts.reform.bulkscanprocessor.tasks.report;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.services.email.ReportSender;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;

import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DailySummaryReportTest {

    @Mock
    private ReportsService service;

    @Mock
    private ReportSender emailSender;

    private DailySummaryReport summaryReport;

    @Before
    public void setUp() {
        summaryReport = new DailySummaryReport(service, emailSender);
    }

    @Test
    public void should_send_report_successfully() {
        // given
        given(service.getZipFilesSummary(now(), null)).willReturn(emptyList());

        // when
        summaryReport.send();

        // then
        verify(emailSender).send();
    }
}
