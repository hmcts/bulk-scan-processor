package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@IntegrationTest
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @MockBean
    private AuthService authService;

    @Test
    void should_update_payment_status() throws Exception {
        List<Payment> payments = asList(
            new Payment("11234"),
            new Payment("22234"),
            new Payment("33234")
        );

        // given
        Envelope envelope = EnvelopeCreator.envelope(payments);
        envelopeRepository.save(envelope);

        List<PaymentInfo> paymentInfoList = Arrays.asList(new PaymentInfo("11234"),
                                                          new PaymentInfo("22234"), new PaymentInfo("33234"));
        //Given
        PaymentRequest paymentRequest = new PaymentRequest(paymentInfoList);

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");

        //When
        mockMvc.perform(put("/payment/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("ServiceAuthorization", "testServiceAuthHeader")
                            .content(new ObjectMapper().writeValueAsString(paymentRequest)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(PaymentController.SUCCESSFUL_UPATE));


        //Then
        verify(authService, times(1)).authenticate("testServiceAuthHeader");
    }


}
