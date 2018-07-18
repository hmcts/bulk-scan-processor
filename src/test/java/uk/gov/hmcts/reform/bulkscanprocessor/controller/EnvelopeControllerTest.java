package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.EnvelopeController;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(EnvelopeController.class)
public class EnvelopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvelopeRetrieverService envelopeRetrieverService;

    @MockBean
    private AuthService authService;

    @Test
    public void should_successfully_return_all_envelopes_for_a_given_jurisdiction() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(authService.authenticate("testServiceAuthHeader")).thenReturn("testServiceName");
        when(envelopeRetrieverService.getAllEnvelopesForJurisdiction("testServiceName")).thenReturn(envelopes);

        mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(expectedEnvelopes()));

        verify(envelopeRetrieverService).getAllEnvelopesForJurisdiction("testServiceName");
        verify(authService).authenticate("testServiceAuthHeader");
    }

    @Test
    public void should_return_status_code_500_when_envelope_retrieval_throws_exception() throws Exception {
        when(authService.authenticate("testServiceAuthHeader")).thenReturn("testServiceName");

        doThrow(new DataRetrievalFailureException("Cannot retrieve data"))
            .when(envelopeRetrieverService).getAllEnvelopesForJurisdiction("testServiceName");

        MvcResult result = this.mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);

        assertThat(result.getResolvedException().getMessage()).isEqualTo("Cannot retrieve data");

        verify(authService).authenticate("testServiceAuthHeader");
    }

    @Test
    public void should_throw_unauthenticated_exception_when_service_auth_header_is_missing() throws Exception {
        when(authService.authenticate(null)).thenThrow(UnAuthenticatedException.class);

        MvcResult result = this.mockMvc.perform(get("/envelopes")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        assertThat(result.getResolvedException()).isInstanceOf(UnAuthenticatedException.class);
    }

    @Test
    public void should_throw_service_jurisdiction_config_not_found_exc_when_service_jurisdiction_mapping_is_not_found()
        throws Exception {
        when(authService.authenticate("testServiceAuthHeader")).thenReturn("test");

        when(envelopeRetrieverService.getAllEnvelopesForJurisdiction("test"))
            .thenThrow(ServiceJuridictionConfigNotFoundException.class);

        MvcResult result = this.mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        assertThat(result.getResolvedException()).isInstanceOf(ServiceJuridictionConfigNotFoundException.class);

        verify(authService).authenticate("testServiceAuthHeader");
    }

    private String expectedEnvelopes() throws IOException {
        URL url = getResource("envelope.json");
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }
}
