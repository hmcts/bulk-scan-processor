package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class PaymentRequest {

    @JsonProperty(value = "payments", required = true)
    @NotEmpty(message = "Payment list can't be empty")
    public List<@Valid PaymentInfo> payments;

    private PaymentRequest() {

    }

    public PaymentRequest(List<PaymentInfo> payments) {
        this.payments = payments;
    }
}
