package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.EnvelopeController;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidStatusChangeException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeUpdateService;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


@RunWith(SpringRunner.class)
@WebMvcTest(EnvelopeController.class)
public class UpdateStatusEnvelopesControllerTest {

    @MockBean private EnvelopeRetrieverService readService; //NOPMD
    @MockBean private EnvelopeUpdateService updateService;
    @MockBean private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_STATUS_UPDATE_REQ_BODY = "{\"status\": \"CONSUMED\"}";

    @Test
    public void should_return_400_if_new_status_is_invalid() throws Exception {
        // when
        MockHttpServletResponse res = sendUpdate("{\"status\": \"something_stupid\"}");

        // then
        assertThat(res.getStatus()).isEqualTo(400);
    }

    @Test
    public void should_return_404_if_envelope_does_not_exist() throws Exception {
        // given
        UUID id = randomUUID();

        doThrow(EnvelopeNotFoundException.class)
            .when(updateService)
            .updateStatus(eq(id), any(), any());

        // when
        MockHttpServletResponse res = sendUpdate(id, VALID_STATUS_UPDATE_REQ_BODY);

        // then
        assertThat(res.getStatus()).isEqualTo(404);
    }

    @Test
    public void should_return_204_if_envelope_was_updated_successfully() throws Exception {
        // given
        // no exception are thrown by services used

        // when
        MockHttpServletResponse res = sendUpdate(VALID_STATUS_UPDATE_REQ_BODY);

        // then
        assertThat(res.getStatus()).isEqualTo(204);
    }

    @Test
    public void should_return_401_if_service_is_not_authenticated() throws Exception {
        // given
        doThrow(InvalidTokenException.class)
            .when(authService)
            .authenticate(any());

        // when
        MockHttpServletResponse res = sendUpdate(VALID_STATUS_UPDATE_REQ_BODY);

        // then
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    public void should_return_403_if_given_status_transition_is_not_allowed() throws Exception {
        // given
        doThrow(InvalidStatusChangeException.class)
            .when(updateService)
            .updateStatus(any(), any(), any());

        // when
        MockHttpServletResponse res = sendUpdate(VALID_STATUS_UPDATE_REQ_BODY);

        // then
        assertThat(res.getStatus()).isEqualTo(403);
    }

    @Test
    public void should_return_403_if_service_tries_to_update_an_envelope_from_other_service() throws Exception {
        // given
        doThrow(ForbiddenException.class)
            .when(updateService)
            .updateStatus(any(), any(), any());

        // when
        MockHttpServletResponse res = sendUpdate(VALID_STATUS_UPDATE_REQ_BODY);

        // then
        assertThat(res.getStatus()).isEqualTo(403);
    }

    private MockHttpServletResponse sendUpdate(String body) throws Exception {
        return sendUpdate(randomUUID(), body);
    }

    private MockHttpServletResponse sendUpdate(UUID id, String body) throws Exception {
        return mockMvc
            .perform(
                put("/envelopes/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            .andReturn()
            .getResponse();
    }
}
