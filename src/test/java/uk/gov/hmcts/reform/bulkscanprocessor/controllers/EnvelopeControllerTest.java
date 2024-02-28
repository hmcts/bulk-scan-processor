package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvelopeController.class)
public class EnvelopeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncompleteEnvelopesService mockIncompleteEnvelopeService;

    @MockBean
    private EnvelopeRetrieverService mockEnvelopeRetrieverService;

    @MockBean
    private AuthService mockAuthService;

    private int defaultStaleTime = 168;
    private final String testUuidOne = "1533e145-bb63-4e7a-9a59-b193cb878ea7";
    private final String testUuidTwo = "1533e145-bb63-4e7a-9a59-b193cb878ea8";
    private final String testUuidThree = "1533e145-bb63-4e7a-9a59-b193cb878ea9";
    private final List<EnvelopeInfo> envelopeInfos = List.of(
        new EnvelopeInfo("container", "file name",
                         UUID.fromString(testUuidOne), Instant.parse("2024-01-18T15:31:45Z")
        ),
        new EnvelopeInfo("container two", "file name two",
                         UUID.fromString(testUuidTwo), Instant.parse("2024-02-19T15:32:46Z")
        ),
        new EnvelopeInfo("container three", "file name three",
                         UUID.fromString(testUuidThree), Instant.parse("2024-03-20T15:33:47Z")
        )
    );

    @Test
    void should_successfully_remove_stale_envelopes() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(defaultStaleTime, List.of(
                      testUuidOne, testUuidTwo, testUuidThree
                  ))).willReturn(1);
        given(mockIncompleteEnvelopeService.getIncompleteEnvelopes(anyInt()))
            .willReturn(envelopeInfos);
        performDeleteOneStaleEnvelopes(defaultStaleTime)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3))
            // mockmvc does not support comparing entire lists, so check each item separately
            .andExpect(jsonPath("$.data", hasItem(testUuidOne)))
            .andExpect(jsonPath("$.data", hasItem(testUuidTwo)))
            .andExpect(jsonPath("$.data", hasItem(testUuidThree)));
    }

    @Test
    void should_fail_to_remove_all_stale_envelopes_when_invalid_stale_time() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(defaultStaleTime, List.of(testUuidOne))).willReturn(1);
        performDeleteOneStaleEnvelopes(1)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("stale_time must be at least 48 hours")));
    }

    @Test
    void should_successfully_remove_stale_envelope() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(defaultStaleTime, List.of(testUuidOne))).willReturn(1);
        performDeleteOneStaleEnvelope(defaultStaleTime, testUuidOne)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.data").value(testUuidOne));
    }

    @Test
    void should_return_not_found_exception_for_not_found_envelope() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(defaultStaleTime, List.of(testUuidOne))).willReturn(0);
        performDeleteOneStaleEnvelope(defaultStaleTime, testUuidOne)
            .andExpect(status().isNotFound());
    }

    @Test
    void should_fail_to_remove_stale_envelope_when_invalid_stale_time() throws Exception {
        given(mockIncompleteEnvelopeService
                  .deleteIncompleteEnvelopes(defaultStaleTime, List.of(testUuidTwo))).willReturn(1);
        performDeleteOneStaleEnvelope(72, testUuidTwo)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("stale_time must be at least 168 hours (a week)")));
    }

    private ResultActions performDeleteOneStaleEnvelope(int staleTime, String envelopeId) throws Exception {
        return mockMvc.perform(delete("/envelopes/stale/{envelopeId}", envelopeId)
                                   .param("stale_time", String.valueOf(staleTime)));
    }

    private ResultActions performDeleteOneStaleEnvelopes(int staleTime) throws Exception {
        return mockMvc.perform(delete("/envelopes/stale/all")
                                   .param("stale_time", String.valueOf(staleTime)));
    }
}
