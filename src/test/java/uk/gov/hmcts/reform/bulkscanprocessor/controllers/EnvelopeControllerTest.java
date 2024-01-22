package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doNothing;
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

    private final int DEFAULT_STALE_TIME = 168;
    private final int INVALID_DEFAULT_STATE_TIME_SINGLE_ENVELOPE = 72;
    private final String testUuidOne = "1533e145-bb63-4e7a-9a59-b193cb878ea7";

    @Test
    void should_successfully_remove_stale_envelope() throws Exception {
        doNothing().when(mockIncompleteEnvelopeService)
            .deleteIncompleteEnvelope(DEFAULT_STALE_TIME, UUID.fromString(testUuidOne));
        performDeleteOneStaleEnvelope(DEFAULT_STALE_TIME, testUuidOne)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.data").value(testUuidOne));
    }

    @Test
    void should_fail_to_remove_stale_envelope_when_invalid_stale_time() throws Exception {
        doNothing().when(mockIncompleteEnvelopeService)
            .deleteIncompleteEnvelope(INVALID_DEFAULT_STATE_TIME_SINGLE_ENVELOPE,
                                      UUID.fromString(testUuidOne));
        performDeleteOneStaleEnvelope(INVALID_DEFAULT_STATE_TIME_SINGLE_ENVELOPE, testUuidOne)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("stale_time must be at least 168 hours (a week)")));
    }

    private ResultActions performDeleteOneStaleEnvelope(int staleTime, String envelopeId) throws Exception {
        return mockMvc.perform(delete("/envelopes/stale/{envelopeId}", envelopeId)
                                   .param("stale_time", String.valueOf(staleTime)));
    }
}
