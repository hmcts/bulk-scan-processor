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
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.EnvelopeRetrieverService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
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

    private static final Timestamp CURRENT_TIMESTAMP = new Timestamp(System.currentTimeMillis());

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnvelopeRetrieverService envelopeRetrieverService;

    @Test
    public void should_successfully_return_all_envelopes() throws Exception {
        List<Envelope> envelopes = EnvelopeCreator.envelopes();

        when(envelopeRetrieverService.getAllEnvelopes()).thenReturn(envelopes);


        mockMvc.perform(get("/envelopes"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().json(expectedEnvelopes()));

        verify(envelopeRetrieverService).getAllEnvelopes();
    }

    @Test
    public void should_return_status_code_500_when_envelope_retrieval_throws_exception() throws Exception {
        doThrow(new DataRetrievalFailureException("Cannot retrieve data"))
            .when(envelopeRetrieverService).getAllEnvelopes();

        MvcResult result = this.mockMvc.perform(get("/envelopes")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(500);

        assertThat(result.getResolvedException().getMessage()).isEqualTo("Cannot retrieve data");
    }

    private String expectedEnvelopes() throws IOException {
        URL url = getResource("envelope.json");
        return Resources.toString(url, Charsets.toCharset("UTF-8"));
    }

}
