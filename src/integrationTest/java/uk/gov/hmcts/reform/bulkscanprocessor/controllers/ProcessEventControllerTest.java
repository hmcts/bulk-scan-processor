package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationContextInitializer;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.config.Profiles;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ProcessEventsService;

import java.time.LocalDate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({
    IntegrationContextInitializer.PROFILE_WIREMOCK,
    Profiles.SERVICE_BUS_STUB,
    Profiles.STORAGE_STUB
})
@AutoConfigureMockMvc
@IntegrationTest
class ProcessEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessEventsService processEventsService;

    @Test
    void should_return_empty_result_if_events_for_given_dncPrefix_and_dates_do_not_exist() throws Exception {
        final String dcnPrefix = "1234567890";

        var fromDate = LocalDate.of(2021, 8, 4);
        var toDate = LocalDate.of(2021, 8, 21);
        given(processEventsService.getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate))
                .willReturn(emptyList());

        mockMvc
                .perform(
                        get("/process-events")
                                .queryParam("dcn_prefix", dcnPrefix)
                                .queryParam("between_dates", "2021-08-04,2021-08-21")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void should_use_earliest_date_as_from_date_when_searching_by_dncPrefix() throws Exception {
        final String dcnPrefix = "123456789§";

        var fromDate = LocalDate.of(2021, 8, 4);
        var toDate = LocalDate.of(2021, 8, 21);
        given(processEventsService.getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate))
                .willReturn(emptyList());

        mockMvc
                .perform(
                        get("/process-events")
                                .queryParam("dcn_prefix", dcnPrefix)
                                .queryParam("between_dates", "2021-08-21,2021-08-04")
                )
                .andDo(print())
                .andExpect(status().isOk());
        verify(processEventsService).getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate);
    }

    @Test
    void should_return_400_when_dncPrefix_shorter_than_10_chars() throws Exception {
        final String dcnPrefix = "123456789";

        mockMvc
                .perform(
                        get("/process-events")
                                .queryParam("dcn_prefix", dcnPrefix)
                                .queryParam("between_dates", "2021-08-21,2021-08-04")
                )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_events_if_events_for_given_dncPrefix_and_dates_exists() throws Exception {

        var e1 = new ProcessEvent(
                "A",
                "file1.zip",
                Event.FILE_VALIDATION_FAILURE
        );
        var e2 = new ProcessEvent(
                "A",
                "file1.zip",
                Event.ZIPFILE_PROCESSING_STARTED
        );

        final String dcnPrefix = "file_567890";

        var fromDate = LocalDate.of(2021, 8, 25);
        var toDate = LocalDate.of(2021, 8, 31);
        given(processEventsService.getProcessEventsByDcnPrefix(anyString(), any(), any()))
                .willReturn(asList(e1, e2));

        mockMvc
                .perform(
                        get("/process-events")
                                .queryParam("dcn_prefix", dcnPrefix)
                                .queryParam("between_dates", "2021-08-25,2021-08-31")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("data[0].container").value("A"))
                .andExpect(jsonPath("data[0].zipFileName").value("file1.zip"))
                .andExpect(jsonPath("data[0].event").value("FILE_VALIDATION_FAILURE"))
                .andExpect(jsonPath("data[1].container").value("A"))
                .andExpect(jsonPath("data[1].zipFileName").value("file1.zip"))
                .andExpect(jsonPath("data[1].event").value("ZIPFILE_PROCESSING_STARTED"));
        verify(processEventsService).getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate);
    }
}
