package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

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
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestUtil;

import java.util.List;

import static java.util.List.of;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@IntegrationTest
public class PaymentControllerTest {
    private static final String PAYMENT_REQUEST_JSON = "testData/payment/payments.json";
    private static final String PAYMENTS_EMPTY_JSON = "testData/payment/payments_empty.json";
    private static final String PAYMENT_DCN_EMPTY_JSON = "testData/payment/payments_dcn_empty.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @MockBean
    private AuthService authService;

    @Test
    void should_update_payment_status() throws Exception {
        List<Payment> payments = of(
            new Payment("11234"),
            new Payment("22234"),
            new Payment("33234")
        );

        //Given
        Envelope envelope = EnvelopeCreator.envelope(payments);
        envelopeRepository.save(envelope);

        List<PaymentInfo> paymentInfoList = of(
            new PaymentInfo("11234"),
            new PaymentInfo("22234"),
            new PaymentInfo("33234")
        );

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");

        String request = TestUtil.fileContentAsString(PAYMENT_REQUEST_JSON);

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(request))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(PaymentController.SUCCESSFUL_UPATE));


        //Then
        verify(authService, times(1)).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_return_status_400_for_empty_payments() throws Exception {
        String request = TestUtil.fileContentAsString(PAYMENTS_EMPTY_JSON);

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(request))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field_name").value("payments"))
            .andExpect(jsonPath("$.errors[0].message").value("Payment list can't be empty"));

        //Then
        verify(authService, never()).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_return_status_400_for_payment_with_empty_dcn() throws Exception {
        String request = TestUtil.fileContentAsString(PAYMENT_DCN_EMPTY_JSON);

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(request))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field_name").value("payments[2].documentControlNumber"))
            .andExpect(jsonPath("$.errors[0].message").value("Document control number is empty or null"));


        //Then
        verify(authService, never()).authenticate("testServiceAuthHeader");
    }


}
