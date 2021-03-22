package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.payment.PaymentService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private PaymentService paymentService;

    @Captor
    ArgumentCaptor<PaymentRequest> paymentRequestArgumentCaptor;

    @Test
    public void should_successfully_update_payment_status() throws Exception {
        List<PaymentInfo> paymentInfoList = Arrays.asList(new PaymentInfo("123"),
            new PaymentInfo("234"), new PaymentInfo("567"));
        //Given
        PaymentRequest paymentRequest = new PaymentRequest(paymentInfoList);

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");

        doNothing().when(paymentService).updatePaymentStatus(eq(paymentRequest));

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(new ObjectMapper().writeValueAsString(paymentRequest)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().string(PaymentController.SUCCESSFUL_UPATE));


        //Then
        verify(authService, times(1)).authenticate(eq("testServiceAuthHeader"));
        verify(paymentService, times(1))
            .updatePaymentStatus(paymentRequestArgumentCaptor.capture());

        assertThat(paymentRequestArgumentCaptor.getValue())
            .usingRecursiveComparison().isEqualTo(paymentRequest);
    }
}
