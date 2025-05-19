package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.EnvelopeController;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ForbiddenException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@WebMvcTest(EnvelopeController.class)
class ReadSingleEnvelopeControllerTest {

    @MockitoBean
    private EnvelopeRetrieverService readService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private IncompleteEnvelopesService incompleteEnvelopesService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_200_if_envelope_exists() throws Exception {
        // given
        UUID envelopeId = randomUUID();
        given(readService.findById(any(), eq(envelopeId)))
            .willReturn(Optional.of(EnvelopeResponseMapper.toEnvelopeResponse(envelope())));

        // when
        MockHttpServletResponse res = sendGet(envelopeId);

        // then
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void should_return_404_if_envelope_does_not_exist() throws Exception {
        // given
        given(readService.findById(any(), any()))
            .willReturn(Optional.empty());

        // when
        MockHttpServletResponse res = sendGet(randomUUID());

        // then
        assertThat(res.getStatus()).isEqualTo(404);
    }

    @Test
    void should_return_403_if_envelope_belongs_to_a_different_service() throws Exception {
        // given
        given(readService.findById(any(), any()))
            .willThrow(new ForbiddenException("msg"));

        // when
        MockHttpServletResponse res = sendGet(randomUUID());

        // then
        assertThat(res.getStatus()).isEqualTo(403);
    }

    @Test
    void should_return_401_if_service_is_not_authenticated() throws Exception {
        // given
        given(authService.authenticate(any()))
            .willThrow(new UnAuthenticatedException("msg"));

        // when
        MockHttpServletResponse res = sendGet(randomUUID());

        // then
        assertThat(res.getStatus()).isEqualTo(401);
    }

    private MockHttpServletResponse sendGet(UUID id) throws Exception {
        return mockMvc
            .perform(get("/envelopes/" + id))
            .andReturn()
            .getResponse();
    }
}
