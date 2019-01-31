package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.ZipStatusController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;
import uk.gov.hmcts.reform.bulkscanprocessor.util.DateFormatter;

import java.sql.Timestamp;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ZipStatusController.class)
public class ZipStatusControllerTest {

    @MockBean
    private ZipFileStatusService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_return_data_returned_from_the_service() throws Exception {

        List<ZipFileEnvelope> envelopes = asList(
            new ZipFileEnvelope("0", "container0", "status0"),
            new ZipFileEnvelope("1", "container1", "status1")
        );

        List<ZipFileEvent> events = asList(
            new ZipFileEvent("type0", "container0", Timestamp.from(now().minusSeconds(10))),
            new ZipFileEvent("type1", "container1", Timestamp.from(now().minusSeconds(15)))
        );

        given(service.getStatusFor("hello.zip")).willReturn(new ZipFileStatus(envelopes, events));

        mockMvc
            .perform(get("/zip-files").param("name", "hello.zip"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.envelopes", hasSize(2)))
            .andExpect(jsonPath("$.envelopes[0].id").value(envelopes.get(0).id))
            .andExpect(jsonPath("$.envelopes[0].container").value(envelopes.get(0).container))
            .andExpect(jsonPath("$.envelopes[0].status").value(envelopes.get(0).status))
            .andExpect(jsonPath("$.envelopes[1].id").value(envelopes.get(1).id))
            .andExpect(jsonPath("$.envelopes[1].container").value(envelopes.get(1).container))
            .andExpect(jsonPath("$.envelopes[1].status").value(envelopes.get(1).status))
            .andExpect(jsonPath("$.events", hasSize(2)))
            .andExpect(jsonPath("$.events[0].type").value(events.get(0).eventType))
            .andExpect(jsonPath("$.events[0].container").value(events.get(0).container))
            .andExpect(jsonPath("$.events[0].created_at").value(toIso(events.get(0).createdAt)))
            .andExpect(jsonPath("$.events[1].type").value(events.get(1).eventType))
            .andExpect(jsonPath("$.events[1].container").value(events.get(1).container))
            .andExpect(jsonPath("$.events[1].created_at").value(toIso(events.get(1).createdAt)));

    }

    @Test
    public void should_return_200_with_empty_model_if_no_results_were_found() throws Exception {
        given(service.getStatusFor("hello.zip")).willReturn(new ZipFileStatus(emptyList(), emptyList()));
        mockMvc
            .perform(get("/zip-files").param("name", "hello.zip"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.envelopes").isEmpty())
            .andExpect(jsonPath("$.events").isEmpty());
    }

    private String toIso(Timestamp timestamp) {
        return DateFormatter.getSimpleDateTime(timestamp.toInstant());
    }
}
