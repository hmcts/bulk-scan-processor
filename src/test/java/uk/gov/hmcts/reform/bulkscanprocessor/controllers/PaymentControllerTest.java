package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentRecordsException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.payment.PaymentService;

import java.util.List;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void should_successfully_update_payment_status() throws Exception {
        List<PaymentInfo> paymentInfoList = of(
            new PaymentInfo("11234"),
            new PaymentInfo("22234"),
            new PaymentInfo("33234")
        );

        //Given
        PaymentRequest paymentRequest = new PaymentRequest(paymentInfoList);

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");

        doNothing().when(paymentService).updatePaymentStatus(paymentRequest);
        String request = Resources.toString(getResource("payment/payments.json"), UTF_8);
        //When
        mockMvc.perform(put("/payment/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("ServiceAuthorization", "testServiceAuthHeader")
                            .content(request))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(PaymentController.SUCCESSFUL_UPDATE));

        ArgumentCaptor<PaymentRequest> paymentRequestArgumentCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        //Then
        verify(authService, times(1)).authenticate("testServiceAuthHeader");
        verify(paymentService, times(1))
            .updatePaymentStatus(paymentRequestArgumentCaptor.capture());

        assertThat(paymentRequestArgumentCaptor.getValue())
            .usingRecursiveComparison().isEqualTo(paymentRequest);
    }

    @Test
    void should_handle_payment_exception() throws Exception {
        List<PaymentInfo> paymentInfoList = of(
            new PaymentInfo("11234"),
            new PaymentInfo("22234"),
            new PaymentInfo("33234")
        );

        //Given
        PaymentRequest paymentRequest = new PaymentRequest(paymentInfoList);

        when(authService.authenticate("testServiceAuthHeader"))
            .thenReturn("testServiceName");

        String message = "Number of records updated don't match";
        doThrow(new PaymentRecordsException(message))
            .when(paymentService).updatePaymentStatus(any());

        String request = Resources.toString(getResource("payment/payments.json"), UTF_8);

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(request))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(message));

        ArgumentCaptor<PaymentRequest> paymentRequestArgumentCaptor = ArgumentCaptor.forClass(PaymentRequest.class);
        //Then
        verify(authService, times(1)).authenticate("testServiceAuthHeader");
        verify(paymentService, times(1))
            .updatePaymentStatus(paymentRequestArgumentCaptor.capture());

        assertThat(paymentRequestArgumentCaptor.getValue())
            .usingRecursiveComparison().isEqualTo(paymentRequest);
    }

    @Test
    void shouldBeAGoodTest() throws Exception {
        mockMvc.perform(get("/payment/testing")
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertThat(1).isEqualTo(1);
    }
}
