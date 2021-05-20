package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.junit.jupiter.api.AfterEach;
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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.AuthService;
import uk.gov.hmcts.reform.bulkscanprocessor.util.TestUtil;

import java.util.List;
import java.util.Optional;

import static java.util.List.of;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    @MockBean
    private PaymentRepository paymentRepository;

    @AfterEach
    public void cleanUp() {
        envelopeRepository.deleteAll();
    }

    @Test
    void should_update_payment_status() throws Exception {
        List<Payment> payments = of(
            new Payment("11234"),
            new Payment("22234"),
            new Payment("33234")
        );
        List<String> dcns = of("11234", "22234", "33234");

        //Given
        Envelope envelope = EnvelopeCreator.envelope(payments);
        envelopeRepository.save(envelope);
        given(paymentRepository.updateStatus(dcns)).willReturn(3);

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
            .andExpect(jsonPath("$.status").value(PaymentController.SUCCESSFUL_UPDATE));

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

    @Test
    void should_get_paymentDcns() throws Exception {
        List<Payment> payments = of(
            new Payment("11234"),
            new Payment("33234")
        );

        List<String> dcns = of("11234", "22234", "33234");
        given(paymentRepository.findByDocumentControlNumberIn(dcns))
            .willReturn(Optional.of(payments));

        mockMvc.perform(get("/payment?dcns=" + String.join(",", dcns)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].documentControlNumber").value("11234"))
            .andExpect(jsonPath("$[1].documentControlNumber").value("33234"));
    }

    @Test
    void should_not_return_payment_when_no_payment() throws Exception {

        List<String> dcns = of("11234", "22234", "33234");
        given(paymentRepository.findByDocumentControlNumberIn(dcns))
            .willReturn(Optional.empty());

        mockMvc.perform(get("/payment?dcns=" + String.join(",", dcns)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").doesNotExist());
    }
}
