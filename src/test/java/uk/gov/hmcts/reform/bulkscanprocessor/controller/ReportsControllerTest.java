package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.ReportsController;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummary;

import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Collections.singletonList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    public void should_return_400_if_date_is_invalid_for_zipfiles_sumamry_endpoint() throws Exception {
        final String invalidDate = "2019-14-14";

        mockMvc
            .perform(get("/reports/zip-files-summary?date=" + invalidDate))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void should_return_zipfiles_summary_result_generated_by_the_service() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.now();

        ZipFileSummary zipFileSummary = new ZipFileSummary(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "BULKSCAN",
            Status.CONSUMED.toString()
        );

        given(reportsService.getZipFilesSummary(localDate, "BULKSCAN"))
            .willReturn(singletonList(zipFileSummary));

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&jurisdiction=BULKSCAN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].file_name").value("test.zip"))
            .andExpect(jsonPath("$.data[0].date_received").value(localDate.toString()))
            .andExpect(jsonPath("$.data[0].time_received").value(localTime.toString()))
            .andExpect(jsonPath("$.data[0].date_processed").value(localDate.toString()))
            .andExpect(jsonPath("$.data[0].time_processed").value(localTime.plusHours(1).toString()))
            .andExpect(jsonPath("$.data[0].jurisdiction").value("BULKSCAN"))
            .andExpect(jsonPath("$.data[0].status").value("CONSUMED"));
    }
}
