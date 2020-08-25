package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.ReportsController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReconciliationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedFilesReportService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;

@WebMvcTest(ReportsController.class)
public class ReportsControllerTest {

    @MockBean
    private ReportsService reportsService;

    @MockBean
    private RejectedFilesReportService rejectedFilesReportService;

    @MockBean
    private ReconciliationService reconciliationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_return_result_generated_by_the_service() throws Exception {
        final EnvelopeCountSummary countSummary = new EnvelopeCountSummary(
            100, 11, "hello", LocalDate.of(2019, 1, 14)
        );

        given(reportsService.getCountFor(countSummary.date, false))
            .willReturn(singletonList(countSummary));

        mockMvc
            .perform(get("/reports/count-summary?date=2019-01-14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].received").value(countSummary.received))
            .andExpect(jsonPath("$.data[0].rejected").value(countSummary.rejected))
            .andExpect(jsonPath("$.data[0].container").value(countSummary.container))
            .andExpect(jsonPath("$.data[0].date").value(countSummary.date.toString()));
    }

    @Test
    public void should_not_include_test_container_by_default() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    public void should_include_test_container_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=true"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    public void should_not_include_test_container_if_exlicitly_not_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=false"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    public void should_return_400_if_date_is_invalid() throws Exception {
        final String invalidDate = "2019-14-14";

        mockMvc
            .perform(get("/reports/count-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_400_if_date_is_invalid_for_zipfiles_summary_endpoint() throws Exception {
        final String invalidDate = "2019-14-14";

        mockMvc
            .perform(get("/reports/zip-files-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }


    @Test
    public void should_return_zipfiles_summary_result_in_csv_format() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse zipFileSummaryResponse = new ZipFileSummaryResponse(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            CONSUMED.toString(),
            COMPLETED.toString(),
            EXCEPTION.name(),
            "ccd-id",
            "ccd-action"
        );

        given(reportsService.getZipFilesSummary(localDate, "bulkscan"))
            .willReturn(singletonList(zipFileSummaryResponse));

        String expectedContent = String.format(
            "Container,Zip File Name,Date Received,Time Received,Date Processed,Time Processed,"
                + "Status,Classification\r\n"
                + "bulkscan,test.zip,%s,%s,%s,%s,CONSUMED,EXCEPTION\r\n",
            localDate.toString(), "12:30:10",
            localDate.toString(), "13:30:10"
        );

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&container=bulkscan")
                         .accept(APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv"))
            .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
            .andExpect(content().string(expectedContent));
    }

    @Test
    public void should_return_empty_zipfiles_summary_in_csv_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "bulkscan"))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14")
                         .accept(APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv"))
            .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
            .andExpect(content().string(
                "Container,Zip File Name,Date Received,Time Received,Date Processed,Time Processed,"
                    + "Status,Classification\r\n"
            ));
    }

    @Test
    public void should_return_zipfiles_summary_result_in_json_format() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse response = new ZipFileSummaryResponse(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            CONSUMED.toString(),
            COMPLETED.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id",
            "ccd-action"
        );

        given(reportsService.getZipFilesSummary(localDate, "bulkscan"))
            .willReturn(singletonList(response));

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&container=bulkscan"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].file_name").value(response.fileName))
            .andExpect(jsonPath("$.data[0].date_received").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_received").value("12:30:10"))
            .andExpect(jsonPath("$.data[0].date_processed").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_processed").value("13:30:10"))
            .andExpect(jsonPath("$.data[0].container").value(response.container))
            .andExpect(jsonPath("$.data[0].last_event_status").value(response.lastEventStatus))
            .andExpect(jsonPath("$.data[0].envelope_status").value(response.envelopeStatus))
            .andExpect(jsonPath("$.data[0].classification").value(response.classification))
            .andExpect(jsonPath("$.data[0].ccd_id").value(response.ccdId))
            .andExpect(jsonPath("$.data[0].ccd_action").value(response.ccdAction));
    }

    @Test
    public void should_return_empty_zipfiles_summary_in_json_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "bulkscan"))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    public void should_return_rejected_files() throws Exception {
        given(rejectedFilesReportService.getRejectedFiles())
            .willReturn(asList(
                new RejectedFile("a.zip", "A"),
                new RejectedFile("b.zip", "B")
            ));

        mockMvc
            .perform(get("/reports/rejected"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{"
                    + "'count': 2,"
                    + "'rejected_files': ["
                    + "  {"
                    + "    'filename': 'a.zip',"
                    + "    'container': 'A'"
                    + "  },"
                    + "  {"
                    + "    'filename': 'b.zip',"
                    + "    'container': 'B'"
                    + "  }"
                    + "]"
                    + "}"
            ));
    }

    @Test
    public void should_return_proper_response_when_there_are_no_rejected_files() throws Exception {
        given(rejectedFilesReportService.getRejectedFiles())
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/rejected"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{'count': 0, 'rejected_files': []}"
            ));
    }

    @Test
    public void should_return_discrepancies_for_a_given_date() throws Exception {
        // given
        String requestBody = "{\"date': '2020-08-20', 'envelopes': []}";

        given(reconciliationService.getReconciliationReport(any(ReconciliationStatement.class)))
            .willReturn(
                singletonList(
                    new Discrepancy(
                        "file1",
                        "c1",
                        DiscrepancyType.PAYMENT_DCNS_MISMATCH.text,
                        "[12345, 23456]",
                        "[12345]"
                    )
                )
            );

        // when
        mockMvc
            .perform(
                post("/reports/reconciliation")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(Resources.toString(getResource("reconciliation/envelopes.json"), UTF_8))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json(Resources.toString(getResource("reconciliation/discrepancies.json"), UTF_8)));

        // then
        ArgumentCaptor<ReconciliationStatement> argument = ArgumentCaptor.forClass(ReconciliationStatement.class);
        verify(reconciliationService).getReconciliationReport(argument.capture());
        ReconciliationStatement reconciliationStatement = argument.getValue();

        assertThat(reconciliationStatement.date).isEqualTo(LocalDate.of(2020, 8, 20));
    }
}
