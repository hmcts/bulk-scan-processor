package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.ZipStatusController;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.ZipFileStatusService;
import uk.gov.hmcts.reform.bulkscanprocessor.util.DateFormatter;

import java.time.Instant;
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

@WebMvcTest(ZipStatusController.class)
public class ZipStatusControllerTest {

    @MockBean
    private ZipFileStatusService service;

    @Autowired
    private MockMvc mockMvc;

    private static final String DIVORCE = "divorce";
    private static final String CMC = "cmc";
    private static final String PROBATE = "probate";

    @Test
    public void should_return_data_returned_from_the_service() throws Exception {

        List<ZipFileEnvelope> envelopes = asList(
            new ZipFileEnvelope(
                "0",
                "container0",
                "status0",
                "9832132131312",
                "AUTO_ATTACHED_TO_CASE",
                false,
                "envelope11.zip",
                Classification.NEW_APPLICATION,
                DIVORCE,
                "1329348437482",
                emptyList(),
                emptyList(),
                emptyList()
            ),
            new ZipFileEnvelope(
                "1",
                "container1",
                "status1",
                "3210329752313",
                "EXCEPTION_RECORD",
                true,
                null,
                Classification.SUPPLEMENTARY_EVIDENCE,
                PROBATE,
                null,
                emptyList(),
                emptyList(),
                emptyList()
            )
        );

        List<ZipFileEvent> events = asList(
            new ZipFileEvent("type0", "container0", now().minusSeconds(10), "reason0"),
            new ZipFileEvent("type1", "container1", now().minusSeconds(15), "reason1")
        );

        given(service.getStatusFor("hello.zip")).willReturn(new ZipFileStatus("hello.zip", envelopes, events));

        mockMvc
            .perform(get("/zip-files").param("name", "hello.zip"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.file_name").value("hello.zip"))
            .andExpect(jsonPath("$.envelopes", hasSize(2)))
            .andExpect(jsonPath("$.envelopes[0].id").value(envelopes.get(0).id))
            .andExpect(jsonPath("$.envelopes[0].container").value(envelopes.get(0).container))
            .andExpect(jsonPath("$.envelopes[0].status").value(envelopes.get(0).status))
            .andExpect(jsonPath("$.envelopes[0].ccd_id").value(envelopes.get(0).ccdId))
            .andExpect(jsonPath("$.envelopes[0].envelope_ccd_action").value(envelopes.get(0).envelopeCcdAction))
            .andExpect(jsonPath("$.envelopes[0].zip_deleted").value(envelopes.get(0).zipDeleted))
            .andExpect(jsonPath("$.envelopes[0].rescan_for").value(envelopes.get(0).rescanFor))
            .andExpect(jsonPath("$.envelopes[0].case_number").value(envelopes.get(0).caseNumber))
            .andExpect(jsonPath("$.envelopes[0].classification")
            .value(envelopes.get(0).classification.toString().toUpperCase()))
            .andExpect(jsonPath("$.envelopes[0].jurisdiction").value(envelopes.get(0).jurisdiction))
            .andExpect(jsonPath("$.envelopes[1].id").value(envelopes.get(1).id))
            .andExpect(jsonPath("$.envelopes[1].container").value(envelopes.get(1).container))
            .andExpect(jsonPath("$.envelopes[1].status").value(envelopes.get(1).status))
            .andExpect(jsonPath("$.envelopes[1].ccd_id").value(envelopes.get(1).ccdId))
            .andExpect(jsonPath("$.envelopes[1].envelope_ccd_action").value(envelopes.get(1).envelopeCcdAction))
            .andExpect(jsonPath("$.envelopes[1].zip_deleted").value(envelopes.get(1).zipDeleted))
            .andExpect(jsonPath("$.envelopes[1].rescan_for").value(envelopes.get(1).rescanFor))
            .andExpect(jsonPath("$.envelopes[1].case_number").value(envelopes.get(1).caseNumber))
            .andExpect(jsonPath("$.envelopes[1].classification")
            .value(envelopes.get(1).classification.toString().toUpperCase()))
            .andExpect(jsonPath("$.envelopes[1].jurisdiction").value(envelopes.get(1).jurisdiction))
            .andExpect(jsonPath("$.events", hasSize(2)))
            .andExpect(jsonPath("$.events[0].type").value(events.get(0).eventType))
            .andExpect(jsonPath("$.events[0].container").value(events.get(0).container))
            .andExpect(jsonPath("$.events[0].created_at").value(toIso(events.get(0).createdAt)))
            .andExpect(jsonPath("$.events[0].reason").value(events.get(0).reason))
            .andExpect(jsonPath("$.events[1].type").value(events.get(1).eventType))
            .andExpect(jsonPath("$.events[1].container").value(events.get(1).container))
            .andExpect(jsonPath("$.events[1].created_at").value(toIso(events.get(1).createdAt)))
            .andExpect(jsonPath("$.events[1].reason").value(events.get(1).reason));
    }

    @Test
    public void should_return_200_with_empty_model_if_no_results_were_found() throws Exception {
        given(service.getStatusFor("hello.zip")).willReturn(new ZipFileStatus("hello.zip", emptyList(), emptyList()));
        mockMvc
            .perform(get("/zip-files").param("name", "hello.zip"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.file_name").value("hello.zip"))
            .andExpect(jsonPath("$.envelopes").isEmpty())
            .andExpect(jsonPath("$.events").isEmpty());
    }

    private String toIso(Instant timestamp) {
        return DateFormatter.getSimpleDateTime(timestamp);
    }
}
