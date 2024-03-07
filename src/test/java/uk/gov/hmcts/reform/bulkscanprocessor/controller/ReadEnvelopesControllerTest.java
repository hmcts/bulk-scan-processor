package uk.gov.hmcts.reform.bulkscanprocessor.controller;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.apache.commons.io.Charsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.bulkscanprocessor.controllers.EnvelopeController;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnAuthenticatedException;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentSubtype;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.IncompleteEnvelopesService;

import java.io.IOException;
import java.net.URL;

import java.util.List;

import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;

@WebMvcTest(EnvelopeController.class)
class ReadEnvelopesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvelopeRetrieverService envelopeRetrieverService;

    @MockBean
    private AuthService authService;

    @MockBean
    private IncompleteEnvelopesService incompleteEnvelopesService;

    @Test
    void should_successfully_return_all_processed_envelopes_for_a_given_jurisdiction() throws Exception {
        List<EnvelopeResponse> envelopes = envelopesInDb();

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");
        when(envelopeRetrieverService.findByServiceAndStatus("testServiceName", UPLOADED))
            .thenReturn(envelopes);

        mockMvc.perform(get("/envelopes?status={status}", UPLOADED.name())
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json(expectedEnvelopes()));

        verify(envelopeRetrieverService).findByServiceAndStatus("testServiceName", UPLOADED);
        verify(authService).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_return_status_code_500_when_envelope_retrieval_throws_exception() throws Exception {
        when(authService.authenticate("testServiceAuthHeader")).thenReturn("testServiceName");

        doThrow(new DataRetrievalFailureException("Cannot retrieve data"))
            .when(envelopeRetrieverService).findByServiceAndStatus(any(), any());

        MvcResult result = this.mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);

        assertThat(result.getResolvedException().getMessage()).isEqualTo("Cannot retrieve data");

        verify(authService).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_throw_unauthenticated_exception_when_service_auth_header_is_missing() throws Exception {
        when(authService.authenticate(null)).thenThrow(UnAuthenticatedException.class);

        MvcResult result = this.mockMvc.perform(get("/envelopes")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);

        assertThat(result.getResolvedException()).isInstanceOf(UnAuthenticatedException.class);
    }

    @Test
    void should_throw_service_jurisdiction_config_not_found_exc_when_service_jurisdiction_mapping_is_not_found()
        throws Exception {
        when(authService.authenticate("testServiceAuthHeader")).thenReturn("test");

        when(envelopeRetrieverService.findByServiceAndStatus(any(), any()))
            .thenThrow(ServiceJuridictionConfigNotFoundException.class);

        MvcResult result = this.mockMvc.perform(get("/envelopes")
            .header("ServiceAuthorization", "testServiceAuthHeader")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);

        assertThat(result.getResolvedException()).isInstanceOf(ServiceJuridictionConfigNotFoundException.class);

        verify(authService).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_not_accept_invalid_statuses_when_reading_envelopes_by_status() throws Exception {
        mockMvc
            .perform(get("/envelopes?status=INVALID_STATUS"))
            .andExpect(status().is(400));
    }

    private List<EnvelopeResponse> envelopesInDb() {
        // by default they are random UUID. need to be specific so the field by field comparison passes
        // it is controller behaviour test after-all
        ScannableItem item1 = EnvelopeCreator.scannableItem("1111001", null, DocumentType.CHERISHED, null);
        ScannableItem item2 = EnvelopeCreator.scannableItem(
            "1111002",
            EnvelopeCreator.ocrData(ImmutableMap.of("name1", "value1")),
            DocumentType.OTHER,
            DocumentSubtype.SSCS1
        );
        item1.setDocumentUuid("0fa1ab60-f836-43aa-8c65-b07cc9bebceb");
        item2.setDocumentUuid("0fa1ab60-f836-43aa-8c65-b07cc9bebcbe");

        Envelope envelope = EnvelopeCreator.envelope(
            "BULKSCAN",
            UPLOADED,
            ImmutableList.of(item1, item2)
        );
        envelope.setZipFileName("7_24-06-2018-00-00-00.zip"); // matches expected response file
        envelope.setRescanFor("1_24-06-2018-00-00-00.zip");
        envelope.setCcdId("ccd-id");
        envelope.setEnvelopeCcdAction("ccd-action");

        return singletonList(EnvelopeResponseMapper.toEnvelopeResponse(envelope));
    }

    private String expectedEnvelopes() throws IOException {
        URL url = getResource("envelope.json");
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }
}
