package uk.gov.hmcts.reform.bulkscanprocessor.model.in;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class PaymentRequest {

    @NotEmpty(message = "Payment list can't be empty")
    private List<@Valid PaymentInfo> payments;

    private PaymentRequest() {
    }

    public PaymentRequest(List<PaymentInfo> payments) {
        this.payments = payments;
    }

    public List<PaymentInfo> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentInfo> payments) {
        this.payments = payments;
    }
}
