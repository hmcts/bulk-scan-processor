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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            .andExpect(jsonPath("$.detail").value("Invalid request content."))
            .andExpect(jsonPath("$.instance").value("/payment/status"));

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
            .andExpect(jsonPath("$.detail").value("Invalid request content."))
            .andExpect(jsonPath("$.instance").value("/payment/status"));

        //Then
        verify(authService, never()).authenticate("testServiceAuthHeader");
    }

    @Test
    void should_get_paymentDcns() throws Exception {
        UUID p1Uuid = UUID.randomUUID();
        String p1Time = "2021-01-03T11:15:30.003Z";
        UUID p2Uuid = UUID.randomUUID();
        String p2Time = "2020-12-20T16:15:30.000Z";

        List<Payment> payments = of(
            new Payment(p1Uuid, "11234", "Done", Instant.parse(p1Time)),
            new Payment(p2Uuid, "33234", "Waiting", Instant.parse(p2Time))
            );

        List<String> dcns = of("11234", "22234", "33234");
        given(paymentRepository.findByDocumentControlNumberIn(dcns))
            .willReturn(Optional.of(payments));

        mockMvc.perform(get("/payment?dcns=" + String.join(",", dcns)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.data[0].id").value(p1Uuid.toString()))
            .andExpect(jsonPath("$.data[0].document_control_number").value("11234"))
            .andExpect(jsonPath("$.data[0].status").value("Done"))
            .andExpect(jsonPath("$.data[0].last_modified").value(p1Time))
            .andExpect(jsonPath("$.data[1].id").value(p2Uuid.toString()))
            .andExpect(jsonPath("$.data[1].document_control_number").value("33234"))
            .andExpect(jsonPath("$.data[1].status").value("Waiting"))
            .andExpect(jsonPath("$.data[1].last_modified").value(p2Time));
    }

    @Test
    void should_not_return_payment_when_no_payment() throws Exception {

        List<String> dcns = of("11234", "22234", "33234");
        given(paymentRepository.findByDocumentControlNumberIn(dcns))
            .willReturn(Optional.empty());

        mockMvc.perform(get("/payment?dcns=" + String.join(",", dcns)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0))
            .andExpect(jsonPath("$.data").isEmpty());
    }
}
