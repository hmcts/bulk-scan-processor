package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PaymentRequest {
    @JsonProperty(value = "payments", required = true)
    public final List<PaymentInfo> payments;

    private PaymentRequest() {
        payments = null;
    }

    public PaymentRequest(List<PaymentInfo> payments) {
        this.payments = payments;
    }
}
