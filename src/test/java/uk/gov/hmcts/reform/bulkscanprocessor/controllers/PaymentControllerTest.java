package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
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
            .andExpect(content().string(PaymentController.SUCCESSFUL_UPATE));


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

        doThrow(new PaymentRecordsException("Number of records updated don't match")).when(paymentService).updatePaymentStatus(any());

        String request = Resources.toString(getResource("payment/payments.json"), UTF_8);

        //When
        mockMvc.perform(put("/payment/status")
            .contentType(MediaType.APPLICATION_JSON)
            .header("ServiceAuthorization", "testServiceAuthHeader")
            .content(request))
            .andDo(print())
            .andExpect(status().isBadRequest());

        //Then
        verify(authService, times(1)).authenticate("testServiceAuthHeader");
        verify(paymentService, times(1))
            .updatePaymentStatus(paymentRequestArgumentCaptor.capture());

        assertThat(paymentRequestArgumentCaptor.getValue())
            .usingRecursiveComparison().isEqualTo(paymentRequest);
    }
}
