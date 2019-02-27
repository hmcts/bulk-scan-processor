package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.ReportsController;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;

import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;

@RunWith(SpringRunner.class)
@WebMvcTest(ReportsController.class)
public class ReportsControllerTest {

    @MockBean
    private ReportsService reportsService;

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
            .andExpect(jsonPath("$.data[0].jurisdiction").value(countSummary.jurisdiction))
            .andExpect(jsonPath("$.data[0].date").value(countSummary.date.toString()));
    }

    @Test
    public void should_not_include_test_jurisdiction_by_default() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    public void should_include_test_jurisdiction_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=true"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    public void should_not_include_test_jurisdiction_if_exlicitly_not_requested_by_the_client() throws Exception {
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
            "BULKSCAN",
            CONSUMED.toString()
        );

        given(reportsService.getZipFilesSummary(localDate, "BULKSCAN"))
            .willReturn(singletonList(zipFileSummaryResponse));

        String expectedContent = String.format(
            "Zip File Name,Date Received,Time Received,Date Processed,Time Processed,Jurisdiction,Status\r\n"
                + "test.zip,%s,%s,%s,%s,BULKSCAN,CONSUMED\r\n",
            localDate.toString(), localTime.toString(),
            localDate.toString(), localTime.plusHours(1).toString()
        );

        mockMvc
            .perform(get("/reports/download-zip-files-summary?date=2019-01-14&jurisdiction=BULKSCAN"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().string(expectedContent));
    }

    @Test
    public void should_return_empty_zipfiles_summary_in_csv_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "BULKSCAN"))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/download-zip-files-summary?date=2019-01-14"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(content().string(
                "Zip File Name,Date Received,Time Received,Date Processed,Time Processed,Jurisdiction,Status\r\n"
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
            "BULKSCAN",
            CONSUMED.toString()
        );

        given(reportsService.getZipFilesSummary(localDate, "BULKSCAN"))
            .willReturn(singletonList(response));

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&jurisdiction=BULKSCAN"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].file_name").value(response.fileName))
            .andExpect(jsonPath("$.data[0].date_received").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_received").value("12:30:10.000"))
            .andExpect(jsonPath("$.data[0].date_processed").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_processed").value("13:30:10.000"))
            .andExpect(jsonPath("$.data[0].jurisdiction").value(response.jurisdiction))
            .andExpect(jsonPath("$.data[0].status").value(response.status));
    }

    @Test
    public void should_return_empty_zipfiles_summary_in_json_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "BULKSCAN"))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

}
